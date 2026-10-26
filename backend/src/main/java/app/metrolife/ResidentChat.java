package app.metrolife;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Short lines residents post to city chat. A line is chosen from what the speaker is doing (and
 * where), the time of day and the city, never one of the last few lines of that city. How often a
 * resident speaks is decided by Community.tick.
 */
final class ResidentChat {

  /** Real time between two resident lines in one city. */
  static final long MIN_GAP_MS = 4 * 60_000,
    MAX_GAP_MS = 7 * 60_000;
  /** A line is not used again while it is among the last this many resident lines of the city. */
  static final int NO_REPEAT = 30;

  static long gap() {
    return ThreadLocalRandom.current().nextLong(MIN_GAP_MS, MAX_GAP_MS + 1);
  }

  static final Map<String, String[]> CITY = Map.of(
    "atl",
    new String[] {
      "The BeltLine is packed today.",
      "Peach cobbler or banana pudding? Asking for me.",
      "Traffic on the Connector again. Taking the train.",
      "Somebody tell me where the good wings are.",
      "Heard there is live music in Little Five later.",
      "Atlanta humidity has its own weather report.",
      "Grabbed boiled peanuts on the way over.",
      "The trees on Ponce look great right now.",
      "MARTA was on time. Writing that down.",
      "Who else is heading to Piedmont Park this weekend?",
    },
    "nyc",
    new String[] {
      "The subway was a sauna this morning.",
      "Best slice is two blocks from wherever you are.",
      "Got a bagel and a seat on the train. Good day.",
      "Somebody is playing sax in the station again. Love it.",
      "Walked forty blocks without noticing.",
      "Bodega cat approved my sandwich choice.",
      "The skyline is doing its thing tonight.",
      "Anyone know a quiet corner in this city?",
      "Ferry ride beats any commute.",
      "Rooftop weather finally.",
    },
    "bay",
    new String[] {
      "Fog rolled in right on schedule.",
      "Burrito the size of my forearm. No regrets.",
      "The bridge is out of the fog for once.",
      "BART was smooth today.",
      "Sourdough from the Ferry Building. Worth the line.",
      "Layers. Always bring layers.",
      "Sea lions are loud at the pier today.",
      "Anyone up for a walk around Lake Merritt?",
      "Hills are a workout even when you are just getting coffee.",
      "Saw the sunset from Dolores. Unreal.",
    }
  );

  static final String[] MORNING = {
    "Coffee first, then people.",
    "Morning, everyone.",
    "Early start today.",
    "The streets are quiet this early. Nice.",
    "Breakfast somewhere new today.",
    "Who else is up already?",
    "Fresh air before work.",
  };
  static final String[] AFTERNOON = {
    "Lunch break could be longer.",
    "Afternoon slump. Need a walk.",
    "Good light for photos right now.",
    "Halfway through the day.",
    "Anyone know a good spot for a late lunch?",
    "Sun is out. Staying outside.",
    "Iced coffee kind of afternoon.",
  };
  static final String[] EVENING = {
    "Long day. Dinner plans?",
    "The city looks great at sunset.",
    "Off work. Finally.",
    "Anyone heading out tonight?",
    "Evening walk to clear my head.",
    "Golden hour on the way home.",
    "Who is cooking tonight?",
  };
  static final String[] NIGHT = {
    "Still awake. Anyone else?",
    "Late night snack run.",
    "The city never quite goes quiet.",
    "Should be asleep by now.",
    "Night walks are underrated.",
    "Streetlights and a good playlist.",
  };

  /** Lines for what the speaker is doing; {place} is the landmark they are at. */
  static final Map<String, String[]> DOING = Map.ofEntries(
    Map.entry(
      "eat",
      new String[] {
        "The food at {place} hits the spot.",
        "Second helping at {place}. No shame.",
        "Trying something new at {place}.",
        "Lunch at {place}. Recommended.",
      }
    ),
    Map.entry(
      "type",
      new String[] {
        "Getting some work done at {place}.",
        "Inbox zero, almost.",
        "One more email and I am done.",
        "Deadline today. Wish me luck.",
      }
    ),
    Map.entry(
      "sit",
      new String[] {
        "Found a nice seat at {place}.",
        "Just sitting and watching people go by.",
        "Taking a breather at {place}.",
        "Good spot to read at {place}.",
      }
    ),
    Map.entry(
      "dance",
      new String[] {
        "The music at {place} is great tonight.",
        "Cannot stop dancing.",
        "Whoever picks the songs at {place}, thank you.",
      }
    ),
    Map.entry(
      "cheer",
      new String[] {
        "What a game at {place}!",
        "The crowd at {place} is loud today.",
        "That last play though.",
        "Lost my voice cheering.",
      }
    ),
    Map.entry(
      "jog",
      new String[] {
        "Out for a run. Legs are complaining.",
        "Quick jog before it gets busy.",
        "Running loop done. Water time.",
        "New personal best on my route.",
      }
    ),
    Map.entry(
      "play",
      new String[] {
        "Playing a little music at {place}.",
        "Practising a new song.",
        "Small crowd at {place}. I will take it.",
      }
    ),
    Map.entry(
      "browse",
      new String[] {
        "Window shopping at {place}.",
        "Found something I did not need. Bought it anyway.",
        "Browsing at {place}. Dangerous.",
      }
    ),
    Map.entry(
      "listen",
      new String[] {
        "Great set at {place}.",
        "This band is better than I expected.",
        "Listening to a talk at {place}. Interesting stuff.",
      }
    ),
    Map.entry(
      "interact",
      new String[] {
        "Busy shift at {place}.",
        "Chatting with folks at {place}.",
        "Running errands around {place}.",
      }
    ),
    Map.entry(
      "walk",
      new String[] {
        "Walking over to {place}.",
        "Taking the long way today.",
        "Just wandering. Good day for it.",
        "Passing by {place}. Busy as always.",
      }
    )
  );

  static String[] timeOfDay(int hour) {
    if (hour >= 5 && hour < 11) return MORNING;
    if (hour >= 11 && hour < 17) return AFTERNOON;
    return hour >= 17 && hour < 22 ? EVENING : NIGHT;
  }

  /**
   * A fresh line for this speaker, or null when every candidate was used recently. Activity lines are
   * preferred, then time-of-day lines, then city lines, so the chat reflects what people are doing.
   */
  static String pick(String city, int hour, String activity, String place, Collection<String> recent) {
    List<String> doing = new ArrayList<>(),
      other = new ArrayList<>();
    for (String line : DOING.getOrDefault(activity, DOING.get("walk")))
      if (place != null && !place.isBlank()) doing.add(line.replace("{place}", place));
      else if (!line.contains("{place}")) doing.add(line);
    other.addAll(Arrays.asList(timeOfDay(hour)));
    other.addAll(Arrays.asList(CITY.getOrDefault(city, new String[0])));
    doing.removeAll(recent);
    other.removeAll(recent);
    var random = ThreadLocalRandom.current();
    // About two in five lines say what the resident is up to; the rest are time and city chatter.
    if (!doing.isEmpty() && (other.isEmpty() || random.nextInt(5) < 2)) return doing.get(random.nextInt(doing.size()));
    return other.isEmpty() ? null : other.get(random.nextInt(other.size()));
  }

  private ResidentChat() {}
}
