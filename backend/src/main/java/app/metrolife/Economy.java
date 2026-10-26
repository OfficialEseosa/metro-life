package app.metrolife;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Itemised, durable money journal and a small daily budget forecast. */
final class Economy {

  final GameService g;

  Economy(GameService g) {
    this.g = g;
  }

  void ensure(ObjectNode s) {
    if (!s.path("ledger").isArray()) {
      s.putArray("ledger");
      s.put("_ledgerBalance", s.path("cash").asDouble());
      append(s, s.path("cash").asDouble(), "Balance when this journal began");
    }
  }

  void append(ObjectNode s, double amount, String reason) {
    if (Math.abs(amount) < .005) return;
    ObjectNode w = (ObjectNode) g.table("worlds").get(s.path("worldId").asText());
    ObjectNode row = s
      .withArray("ledger")
      .addObject()
      .put("id", g.id())
      .put("at", g.now())
      .put("day", w == null ? 1 : g.civic.day(w))
      .put("amount", Math.round(amount * 100) / 100.0)
      .put("reason", reason)
      .put("balance", Math.round(s.path("cash").asDouble() * 100) / 100.0);
    if (w != null) row.set("clock", g.clock(w));
    ArrayNode rows = s.withArray("ledger");
    while (rows.size() > 300) rows.remove(0);
  }

  /** Called after a money mutation. Splitting a shift into cost/pay/reward keeps each reason visible. */
  void record(ObjectNode s, double amount, String reason) {
    if (!s.path("ledger").isArray()) {
      s.putArray("ledger");
      double before = s.path("cash").asDouble() - amount;
      s.put("_ledgerBalance", before);
      append(s, before, "Balance when this journal began");
    }
    append(s, amount, reason);
    s.put("_ledgerBalance", s.path("cash").asDouble());
  }

  void capture(ObjectNode s) {
    ensure(s);
    double change = s.path("cash").asDouble() - s.path("_ledgerBalance").asDouble(s.path("cash").asDouble());
    if (Math.abs(change) > .005) {
      String reason = s.path("_moneyReason").asText();
      if (reason.isEmpty()) reason = change > 0 ? "Goal, story or milestone reward" : "Activity or household expense";
      record(s, change, reason);
    }
    s.remove("_moneyReason");
  }

  void credit(ObjectNode s, double amount, String reason) {
    capture(s);
    s.put("cash", s.path("cash").asDouble() + amount);
    s.withObject("moneyToday").put("income", s.path("moneyToday").path("income").asDouble() + amount);
    record(s, amount, reason);
  }

  void decayNeeds(ObjectNode s, double hours) {
    for (int i = 0; i < Content.NEEDS.length; i++) {
      String need = Content.NEEDS[i];
      s.withObject("needs").put(
        need,
        Math.max(
          0,
          s.path("needs").path(need).asDouble(85) -
            Content.NEED_RATES[i] * hours * Personality.needDecayMultiplier(s, need)
        )
      );
    }
  }

  ObjectNode read(ObjectNode s, ObjectNode w) {
    capture(s);
    ObjectNode out = g.object();
    out.set("entries", s.path("ledger").deepCopy());
    double rent = s.path("home").path("rent").asDouble(),
      food = Content.DAILY_FOOD,
      pay = 0;
    JsonNode job = s.path("job");
    if (job.isObject()) {
      pay = job.path("payPerHour").asDouble(job.path("hourlyPay").asDouble(job.path("pay").asDouble()));
      if (pay == 0) {
        String track = job.path("track").asText();
        for (JsonNode career : g.content.world.path("careers"))
          if (career.path("track").asText().equals(track)) for (JsonNode level : career.path("levels"))
            if (level.path("level").asInt() == job.path("level").asInt()) pay = level.path("payPerHour").asDouble();
      }
    }
    int day = g.civic.day(w);
    long next = g.civic.dayAt(w, day + 1);
    boolean low = s.path("cash").asDouble() < rent + food + s.path("rentDebt").asDouble();
    ObjectNode forecast = out
      .putObject("forecast")
      .put("rent", rent)
      .put("food", food)
      .put("expectedPay", pay)
      .put("dailyNet", pay - rent - food)
      .put("nextRentAt", next)
      .put("rentDebt", s.path("rentDebt").asDouble())
      .put("lowRent", low)
      .put(
        "warning",
        low ? "Your balance may not cover the next rent and meals. Work a shift or choose a cheaper home." : ""
      );
    forecast.put(
      "payAssumption",
      job.isObject() ? "One healthy shift; actual pay depends on your job and mood" : "No job income assumed"
    );
    homes(s, forecast, pay, food);
    ObjectNode rates = out.putObject("ratesPerGameHour");
    for (int i = 0; i < Content.NEEDS.length; i++) rates.put(Content.NEEDS[i], Content.NEED_RATES[i]);
    return out;
  }

  /** Saving left after rent and meals that makes a home sustainable on one shift a day. */
  static double minimumSaving(double pay) {
    return Math.max(10, Math.round(pay * .15));
  }

  /**
   * What each home in the player's home city would leave per day on one healthy shift and two meals
   * out, and the one upgrade worth suggesting: the cheapest home dearer than the current one that
   * still leaves a real saving. No job, or no such home, means no suggestion.
   */
  void homes(ObjectNode s, ObjectNode forecast, double pay, double food) {
    String city = s.path("home").path("city").asText(s.path("city").asText());
    double current = s.path("home").path("rent").asDouble(),
      saving = minimumSaving(pay);
    boolean studio = s.path("home").path("kind").asText().equals("studio");
    ArrayNode homes = forecast.putArray("homes");
    ObjectNode upgrade = null;
    for (JsonNode nh : g.content.city(city).path("neighborhoods")) {
      double rent = nh.path("rent").asDouble(),
        net = pay - rent - food;
      boolean here = nh.path("id").asText().equals(s.path("home").path("neighborhood").asText()) && !studio,
        sustainable = pay > 0 && net >= saving;
      ObjectNode row = homes
        .addObject()
        .put("city", city)
        .put("neighborhood", nh.path("id").asText())
        .put("name", nh.path("name").asText())
        .put("rent", rent)
        .put("dailyNet", net)
        .put("sustainable", sustainable)
        .put("current", here);
      if (!here && sustainable && rent > current && (upgrade == null || rent < upgrade.path("rent").asDouble())) upgrade =
        row;
    }
    if (upgrade == null) {
      forecast.putNull("upgrade");
      return;
    }
    double deposit = upgrade.path("rent").asDouble(),
      // The deposit plus one more day of rent and meals, so moving never leaves the player short.
      needed = deposit + upgrade.path("rent").asDouble() + food + s.path("rentDebt").asDouble();
    forecast
      .putObject("upgrade")
      .put("city", city)
      .put("neighborhood", upgrade.path("neighborhood").asText())
      .put("name", upgrade.path("name").asText())
      .put("rent", upgrade.path("rent").asDouble())
      .put("deposit", deposit)
      .put("dailyNet", upgrade.path("dailyNet").asDouble())
      .put("cashNeeded", needed)
      .put("ready", s.path("cash").asDouble() >= needed);
  }
}
