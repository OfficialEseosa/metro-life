package app.metrolife;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** World-bound subscriptions. No network write holds the simulation monitor. */
@Configuration
@EnableWebSocket
@EnableScheduling
public class LiveWire extends TextWebSocketHandler implements WebSocketConfigurer {

  /** Same allow-list as the HTTP API (metro.origins / METRO_ORIGINS). */
  @org.springframework.beans.factory.annotation.Value("${metro.ws.origins}")
  String origins;

  long lastCheckpoint;
  final GameService g;
  final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
  final ExecutorService sends = Executors.newVirtualThreadPerTaskExecutor();

  static final class Client {

    final WebSocketSession ws;
    final String token, wid;
    String uid = "",
      city = "atl",
      presence = "",
      positions = "",
      sim = "",
      clock = "";
    final Set<String> chatSeen = new LinkedHashSet<>();
    /**
     * One-time frames (hello, chat, social moments, events) in order. They are never discarded: a
     * slow reader receives every one of them, ahead of any snapshot frame.
     */
    final Queue<String> outgoing = new ConcurrentLinkedQueue<>();
    final AtomicBoolean draining = new AtomicBoolean();
    final Map<String, Integer> resources = new HashMap<>();
    /** Snapshot frames: only the newest of each is worth sending; older ones are replaced, never queued. */
    final AtomicReference<String> latestPresence = new AtomicReference<>(),
      latestClock = new AtomicReference<>(),
      latestPositions = new AtomicReference<>();
    long clockSentAt;

    Client(WebSocketSession ws, String token, String wid) {
      this.ws = new ConcurrentWebSocketSessionDecorator(ws, 2000, 65536);
      this.token = token;
      this.wid = wid;
    }
  }

  /**
   * One-time frames a client may fall behind by. Snapshot frames never count (they are coalesced),
   * so this is only reached by a reader that has stopped reading: its socket is closed, and the
   * reconnect reloads everything over HTTP and replays the social inbox (see replaySocial).
   */
  static final int MAX_QUEUED = 2048;
  /** Received social moments younger than this are replayed to every newly opened socket. */
  static final long SOCIAL_REPLAY_MS = 10 * 60_000;
  /** Heartbeat: a clock frame at least this often, so clients can detect a dead link. */
  static final long CLOCK_HEARTBEAT_MS = 5000;

  LiveWire(GameService g) {
    this.g = g;
    g.userFrames = this::sendToUser;
  }

  /** Deliver a frame to every open socket of one user (called under the simulation monitor; never writes inline). */
  void sendToUser(String wid, String uid, String frame) {
    for (Client c : clients.values()) if (c.wid.equals(wid) && c.uid.equals(uid) && c.ws.isOpen()) send(c, frame);
  }

  /** Cookie-less fallback: the page passes its session as ?session= when the browser blocks the API cookie. */
  static String querySession(ServerHttpRequest request) {
    String query = request.getURI().getRawQuery();
    if (query == null) return "";
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0 && pair.substring(0, eq).equals("session")) {
        String value = java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
        return SessionHeaderFilter.validToken(value) ? value : "";
      }
    }
    return "";
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(this, "/ws").setAllowedOriginPatterns(WebConfig.split(origins)).addInterceptors(
      new HandshakeInterceptor() {
        public boolean beforeHandshake(
          ServerHttpRequest request,
          ServerHttpResponse response,
          WebSocketHandler handler,
          Map<String, Object> attributes
        ) {
          String token = "";
          for (String cookie : request.getHeaders().getOrEmpty("Cookie"))
            for (String part : cookie.split(";"))
              if (part.trim().startsWith("ml_session=")) token = part.trim().substring(11);
          if (token.isEmpty()) token = querySession(request);
          synchronized (g) {
            ObjectNode session = g.session(token, false);
            if (session == null) {
              response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
              return false;
            }
            attributes.put("token", token);
            attributes.put("wid", g.world(session).path("id").asText());
            return true;
          }
        }

        public void afterHandshake(
          ServerHttpRequest request,
          ServerHttpResponse response,
          WebSocketHandler handler,
          Exception exception
        ) {}
      }
    );
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    Client c = new Client(
      session,
      (String) session.getAttributes().get("token"),
      (String) session.getAttributes().get("wid")
    );
    clients.put(session.getId(), c);
    synchronized (g) {
      ObjectNode ses = g.session(c.token, true),
        w = g.world(ses);
      g.community.touch(w);
      String uid = ses.path("userId").asText();
      c.uid = uid;
      JsonNode s = g.table("sims").get(g.simKey(ses));
      if (s instanceof ObjectNode sim) {
        g.navigation.resumePosture(sim);
        c.city = sim.path("city").asText();
      }
      g.community.online.put(c.wid + ":" + uid, g.now());
      send(c, g.object().put("type", "hello").put("worldId", c.wid).set("clock", g.clock(w)).toString());
      if (s instanceof ObjectNode sim) replaySocial(c, sim);
    }
  }

  /**
   * A socket that was closed, dropped or not yet open when a social moment arrived must still hear
   * about it: every new socket gets the recent inbox again, and the page ignores what it has seen.
   */
  void replaySocial(Client c, ObjectNode sim) {
    for (JsonNode entry : sim.path("socialInbox"))
      if (entry.path("at").asLong() >= g.now() - SOCIAL_REPLAY_MS) send(c, GameService.socialFrame(entry, true));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Client c = clients.get(session.getId());
    if (c == null) return;
    if (message.getPayloadLength() > 1024) {
      session.close(CloseStatus.TOO_BIG_TO_PROCESS);
      return;
    }
    try {
      JsonNode frame = g.mapper.readTree(message.getPayload());
      if (frame.path("type").asText().equals("view")) {
        String city = frame.path("city").asText();
        synchronized (g) {
          g.content.city(city);
          c.city = city;
          c.presence = "";
          c.positions = "";
          c.resources.clear();
          c.chatSeen.clear();
        }
      } else if (!frame.path("type").asText().equals("ping")) session.close(CloseStatus.BAD_DATA);
    } catch (Exception e) {
      session.close(CloseStatus.BAD_DATA);
    }
  }

  void drop(Client c) {
    clients.values().remove(c);
    synchronized (g) {
      if (
        clients
          .values()
          .stream()
          .noneMatch(other -> other.wid.equals(c.wid) && other.uid.equals(c.uid) && other.ws.isOpen())
      ) g.community.online.remove(c.wid + ":" + c.uid);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Client c = clients.get(session.getId());
    if (c != null) drop(c);
  }

  /**
   * Queue a frame. Presence and clock frames are snapshots: an unsent one is replaced by the newer
   * one. Every other frame is one-time and is kept until it is written; an identical pending
   * "event" frame (a re-read trigger) is not queued twice.
   */
  void send(Client c, String frame) {
    if (frame.startsWith("{\"type\":\"presence\"")) c.latestPresence.set(frame);
    else if (frame.startsWith("{\"type\":\"clock\"")) c.latestClock.set(frame);
    else if (frame.startsWith("{\"type\":\"event\"") && c.outgoing.contains(frame)) return;
    else if (c.outgoing.size() >= MAX_QUEUED) {
      // The page has stopped reading. Close it; the reconnect resynchronises everything.
      drop(c);
      sends.submit(() -> {
        try {
          c.ws.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (Exception ignored) {}
      });
      return;
    } else c.outgoing.add(frame);
    drain(c);
  }

  /** Replace any unsent positions frame with the newest one. */
  void sendPositions(Client c, String frame) {
    c.latestPositions.set(frame);
    drain(c);
  }

  /** One-time frames first, then the newest roster, clock and positions. */
  String nextFrame(Client c) {
    String text = c.outgoing.poll();
    if (text == null) text = c.latestPresence.getAndSet(null);
    if (text == null) text = c.latestClock.getAndSet(null);
    return text != null ? text : c.latestPositions.getAndSet(null);
  }

  boolean pending(Client c) {
    return (
      !c.outgoing.isEmpty() ||
      c.latestPresence.get() != null ||
      c.latestClock.get() != null ||
      c.latestPositions.get() != null
    );
  }

  void drain(Client c) {
    if (!c.draining.compareAndSet(false, true)) return;
    sends.submit(() -> {
      try {
        String text;
        while ((text = nextFrame(c)) != null) if (c.ws.isOpen()) c.ws.sendMessage(new TextMessage(text));
      } catch (Exception e) {
        drop(c);
        try {
          c.ws.close(CloseStatus.SESSION_NOT_RELIABLE);
        } catch (Exception ignored) {}
      } finally {
        c.draining.set(false);
        if (pending(c) && c.ws.isOpen()) drain(c);
      }
    });
  }

  @Scheduled(fixedDelay = 200)
  public void motionTick() {
    List<Map.Entry<Client, String>> frames = new ArrayList<>();
    synchronized (g) {
      g.residentMotion.tick();
      for (String wid : clients
        .values()
        .stream()
        .map(c -> c.wid)
        .distinct()
        .toList())
        g.community.preparePresence(wid);
      for (Client c : clients.values()) {
        ObjectNode ses = g.session(c.token, false);
        if (ses == null || !g.world(ses).path("id").asText().equals(c.wid)) continue;
        var points = g.mapper.createArrayNode();
        for (JsonNode table : List.of(g.table("sims"), g.community.residents().path(c.wid)))
          for (JsonNode row : table)
            if (
              row.path("worldId").asText().equals(c.wid) &&
              row.path("city").asText().equals(c.city) &&
              row.has("position") &&
              (row.path("npc").asBoolean() || g.community.isOnline(c.wid, row.path("userId").asText()))
            ) {
              ObjectNode p = points
                .addObject()
                .put("socialBusy", g.community.socialBusy(row))
                .put("id", row.path("userId").asText())
                .put("loc", row.path("loc").asText())
                .put(
                  "activity",
                  row.path("npc").asBoolean()
                    ? row.path("activity").asText("walk")
                    : row.path("busyKind").asText("idle")
                );
              g.community.visibleLife(p, row);
              p.put("travelStatus", g.transit.status(row));
              if (g.transit.active(row)) p.put("tripId", row.path("trip").path("id").asText());
              else p.putNull("tripId");
              p.set("position", row.path("position").deepCopy());
              p.set(
                "activitySlot",
                row.path("activitySlot").isMissingNode() ? g.mapper.nullNode() : row.path("activitySlot").deepCopy()
              );
            }
        String text = points.toString();
        if (!text.equals(c.positions)) {
          c.positions = text;
          ObjectNode f = g.object().put("type", "positions").put("city", c.city).put("at", g.now());
          f.set("players", points);
          frames.add(Map.entry(c, f.toString()));
        }
      }
    }
    for (var frame : frames) sendPositions(frame.getKey(), frame.getValue());
  }

  @Scheduled(fixedDelay = 1000)
  public void tick() {
    List<Map.Entry<Client, String>> frames = new ArrayList<>();
    List<Client> invalid = new ArrayList<>();
    synchronized (g) {
      for (Client c : clients.values()) {
        ObjectNode session = g.session(c.token, false);
        if (!c.ws.isOpen() || session == null || !g.world(session).path("id").asText().equals(c.wid)) {
          invalid.add(c);
          continue;
        }
        g.community.online.put(c.wid + ":" + session.path("userId").asText(), g.now());
        g.community.activeWorlds.put(c.wid, g.now());
      }
      for (JsonNode world : g.table("worlds")) g.transit.tick((ObjectNode) world);
      g.community.tick();
      for (Client c : clients.values()) {
        if (invalid.contains(c)) continue;
        ObjectNode ses = g.session(c.token, false),
          w = g.world(ses);
        ObjectNode p = g.community.players(w, c.city);
        String presence = p.path("players").toString();
        if (!presence.equals(c.presence)) {
          c.presence = presence;
          ObjectNode f = g.object().put("type", "presence").put("city", c.city);
          f.set("players", p.path("players"));
          frames.add(Map.entry(c, f.toString()));
        }
        JsonNode time = g.clock(w);
        String clock = time.path("day").asText() + ":" + time.path("hour").asText();
        if (!clock.equals(c.clock) || g.now() - c.clockSentAt >= CLOCK_HEARTBEAT_MS) {
          c.clock = clock;
          c.clockSentAt = g.now();
          ObjectNode f = g.object().put("type", "clock");
          f.set("clock", time);
          frames.add(Map.entry(c, f.toString()));
        }
        JsonNode s = g.table("sims").get(g.simKey(ses));
        if (s instanceof ObjectNode sim) {
          g.decay(sim, w);
          String key =
            sim.path("cash") +
            ":" +
            sim.path("busyUntil") +
            ":" +
            sim.path("loc") +
            ":" +
            sim.path("pendingBeat") +
            ":" +
            sim.path("job") +
            ":" +
            sim.path("aspiration") +
            ":" +
            sim.path("_socialAt") +
            ":" +
            g.transit.trip(sim);
          if (!key.equals(c.sim)) {
            c.sim = key;
            // "sim": your own sim changed, re-read /api/sim. Received social moments arrive as
            // their own "socialReceived" frame (see GameService.social).
            ObjectNode f = g.object().put("type", "event").put("kind", "sim").put("city", sim.path("city").asText());
            frames.add(Map.entry(c, f.toString()));
          }
        }
        for (String kind : List.of("mayor", "plots", "billboards", "homes")) {
          JsonNode resource = kind.equals("mayor")
            ? g.civic.state(w, c.city)
            : kind.equals("homes")
              ? g.property.homes(c.token, c.city)
              : g.property.state(w, c.city).path(kind.equals("plots") ? "plots" : "billboards");
          int hash = resource.hashCode();
          Integer old = c.resources.put(kind, hash);
          if (old == null || old != hash) frames.add(
            Map.entry(c, g.object().put("type", "event").put("kind", kind).put("city", c.city).toString())
          );
        }
        for (JsonNode m : s instanceof ObjectNode actor && g.transit.active(actor)
          ? g.transit.history(actor)
          : g.community.history(w, c.city)) {
          if (c.chatSeen.add(m.path("id").asText())) {
            ObjectNode f = g.object().put("type", "chat");
            f.set("message", m);
            frames.add(Map.entry(c, f.toString()));
          }
        }
        while (c.chatSeen.size() > 100) c.chatSeen.remove(c.chatSeen.iterator().next());
      }
      if (g.now() - lastCheckpoint >= 5000 && (!clients.isEmpty() || !g.community.activeWorlds.isEmpty())) {
        g.checkpoint();
        lastCheckpoint = g.now();
      }
    }
    for (Client c : invalid) {
      drop(c);
      sends.submit(() -> {
        try {
          c.ws.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception ignored) {}
      });
    }
    for (var frame : frames) send(frame.getKey(), frame.getValue());
  }

  @PreDestroy
  void close() {
    for (Client c : clients.values())
      try {
        c.ws.close();
      } catch (Exception ignored) {}
    sends.shutdownNow();
  }
}
