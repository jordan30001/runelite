package net.runelite.client.plugins.chess;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Utils {

	public static final Map<String, Color> COLOR_MAP = new HashMap<>();

	static {
		String[] colors = new String[] { "Black", "#000000", "Night", "#0C090A", "Gunmetal", "#2C3539", "Midnight",
				"#2B1B17", "Charcoal", "#34282C", "Dark Slate Grey", "#25383C", "Oil", "#3B3131", "Black Cat",
				"#413839", "Iridium", "#3D3C3A", "Black Eel", "#463E3F", "Black Cow", "#4C4646", "Gray Wolf", "#504A4B",
				"Vampire Gray", "#565051", "Gray Dolphin", "#5C5858", "Carbon Gray", "#625D5D", "Ash Gray", "#666362",
				"Cloudy Gray", "#6D6968", "Smokey Gray", "#726E6D", "Gray", "#736F6E", "Granite", "#837E7C",
				"Battleship Gray", "#848482", "Gray Cloud", "#B6B6B4", "Gray Goose", "#D1D0CE", "Platinum", "#E5E4E2",
				"Metallic Silver", "#BCC6CC", "Blue Gray", "#98AFC7", "Light Slate Gray", "#6D7B8D", "Slate Gray",
				"#657383", "Jet Gray", "#616D7E", "Mist Blue", "#646D7E", "Marble Blue", "#566D7E", "Slate Blue",
				"#737CA1", "Steel Blue", "#4863A0", "Blue Jay", "#2B547E", "Dark Slate Blue", "#2B3856",
				"Midnight Blue", "#151B54", "Navy Blue", "#000080", "Blue Whale", "#342D7E", "Lapis Blue", "#15317E",
				"Denim Dark Blue", "#151B8D", "Earth Blue", "#0000A0", "Cobalt Blue", "#0020C2", "Blueberry Blue",
				"#0041C2", "Sapphire Blue", "#2554C7", "Blue Eyes", "#1569C7", "Royal Blue", "#2B60DE", "Blue Orchid",
				"#1F45FC", "Blue Lotus", "#6960EC", "Light Slate Blue", "#736AFF", "Windows Blue", "#357EC7",
				"Glacial Blue Ice", "#368BC1", "Silk Blue", "#488AC7", "Blue Ivy", "#3090C7", "Blue Koi", "#659EC7",
				"Columbia Blue", "#87AFC7", "Baby Blue", "#95B9C7", "Light Steel Blue", "#728FCE", "Ocean Blue",
				"#2B65EC", "Blue Ribbon", "#306EFF", "Blue Dress", "#157DEC", "Dodger Blue", "#1589FF",
				"Cornflower Blue", "#6495ED", "Sky Blue", "#6698FF", "Butterfly Blue", "#38ACEC", "Iceberg", "#56A5EC",
				"Crystal Blue", "#5CB3FF", "Deep Sky Blue", "#3BB9FF", "Denim Blue", "#79BAEC", "Light Sky Blue",
				"#82CAFA", "Day Sky Blue", "#82CAFF", "Jeans Blue", "#A0CFEC", "Blue Angel", "#B7CEEC", "Pastel Blue",
				"#B4CFEC", "Sea Blue", "#C2DFFF", "Powder Blue", "#C6DEFF", "Coral Blue", "#AFDCEC", "Light Blue",
				"#ADDFFF", "Robin Egg Blue", "#BDEDFF", "Pale Blue Lily", "#CFECEC", "Light Cyan", "#E0FFFF", "Water",
				"#EBF4FA", "AliceBlue", "#F0F8FF", "Azure", "#F0FFFF", "Light Slate", "#CCFFFF", "Light Aquamarine",
				"#93FFE8", "Electric Blue", "#9AFEFF", "Aquamarine", "#7FFFD4", "Cyan or Aqua", "#00FFFF", "Tron Blue",
				"#7DFDFE", "Blue Zircon", "#57FEFF", "Blue Lagoon", "#8EEBEC", "Celeste", "#50EBEC", "Blue Diamond",
				"#4EE2EC", "Tiffany Blue", "#81D8D0", "Cyan Opaque", "#92C7C7", "Blue Hosta", "#77BFC7",
				"Northern Lights Blue", "#78C7C7", "Medium Turquoise", "#48CCCD", "Turquoise", "#43C6DB", "Jellyfish",
				"#46C7C7", "Blue green", "#7BCCB5", "Macaw Blue Green", "#43BFC7", "Light Sea Green", "#3EA99F",
				"Dark Turquoise", "#3B9C9C", "Sea Turtle Green", "#438D80", "Medium Aquamarine", "#348781",
				"Greenish Blue", "#307D7E", "Grayish Turquoise", "#5E7D7E", "Beetle Green", "#4C787E", "Teal",
				"#008080", "Sea Green", "#4E8975", "Camouflage Green", "#78866B", "Sage Green", "#848b79",
				"Hazel Green", "#617C58", "Venom Green", "#728C00", "Fern Green", "#667C26", "Dark Forest Green",
				"#254117", "Medium Sea Green", "#306754", "Medium Forest Green", "#347235", "Seaweed Green", "#437C17",
				"Pine Green", "#387C44", "Jungle Green", "#347C2C", "Shamrock Green", "#347C17", "Medium Spring Green",
				"#348017", "Forest Green", "#4E9258", "Green Onion", "#6AA121", "Spring Green", "#4AA02C", "Lime Green",
				"#41A317", "Clover Green", "#3EA055", "Green Snake", "#6CBB3C", "Alien Green", "#6CC417", "Green Apple",
				"#4CC417", "Yellow Green", "#52D017", "Kelly Green", "#4CC552", "Zombie Green", "#54C571", "Frog Green",
				"#99C68E", "Green Peas", "#89C35C", "Dollar Bill Green", "#85BB65", "Dark Sea Green", "#8BB381",
				"Iguana Green", "#9CB071", "Avocado Green", "#B2C248", "Pistachio Green", "#9DC209", "Salad Green",
				"#A1C935", "Hummingbird Green", "#7FE817", "Nebula Green", "#59E817", "Stoplight Go Green", "#57E964",
				"Algae Green", "#64E986", "Jade Green", "#5EFB6E", "Green", "#00FF00", "Emerald Green", "#5FFB17",
				"Lawn Green", "#87F717", "Chartreuse", "#8AFB17", "Dragon Green", "#6AFB92", "Mint green", "#98FF98",
				"Green Thumb", "#B5EAAA", "Light Jade", "#C3FDB8", "Tea Green", "#CCFB5D", "Green Yellow", "#B1FB17",
				"Slime Green", "#BCE954", "Goldenrod", "#EDDA74", "Harvest Gold", "#EDE275", "Sun Yellow", "#FFE87C",
				"Yellow", "#FFFF00", "Corn Yellow", "#FFF380", "Parchment", "#FFFFC2", "Cream", "#FFFFCC",
				"Lemon Chiffon", "#FFF8C6", "Cornsilk", "#FFF8DC", "Beige", "#F5F5DC", "Blonde", "#FBF6D9",
				"AntiqueWhite", "#FAEBD7", "Champagne", "#F7E7CE", "BlanchedAlmond", "#FFEBCD", "Vanilla", "#F3E5AB",
				"Tan Brown", "#ECE5B6", "Peach", "#FFE5B4", "Mustard", "#FFDB58", "Rubber Ducky Yellow", "#FFD801",
				"Bright Gold", "#FDD017", "Golden brown", "#EAC117", "Macaroni and Cheese", "#F2BB66", "Saffron",
				"#FBB917", "Beer", "#FBB117", "Cantaloupe", "#FFA62F", "Bee Yellow", "#E9AB17", "Brown Sugar",
				"#E2A76F", "BurlyWood", "#DEB887", "Deep Peach", "#FFCBA4", "Ginger Brown", "#C9BE62",
				"School Bus Yellow", "#E8A317", "Sandy Brown", "#EE9A4D", "Fall Leaf Brown", "#C8B560", "Orange Gold",
				"#D4A017", "Sand", "#C2B280", "Cookie Brown", "#C7A317", "Caramel", "#C68E17", "Brass", "#B5A642",
				"Khaki", "#ADA96E", "Camel brown", "#C19A6B", "Bronze", "#CD7F32", "Tiger Orange", "#C88141",
				"Cinnamon", "#C58917", "Bullet Shell", "#AF9B60", "Dark Goldenrod", "#AF7817", "Copper", "#B87333",
				"Wood", "#966F33", "Oak Brown", "#806517", "Moccasin", "#827839", "Army Brown", "#827B60", "Sandstone",
				"#786D5F", "Mocha", "#493D26", "Taupe", "#483C32", "Coffee", "#6F4E37", "Brown Bear", "#835C3B",
				"Red Dirt", "#7F5217", "Sepia", "#7F462C", "Orange Salmon", "#C47451", "Rust", "#C36241", "Red Fox",
				"#C35817", "Chocolate", "#C85A17", "Sedona", "#CC6600", "Papaya Orange", "#E56717", "Halloween Orange",
				"#E66C2C", "Pumpkin Orange", "#F87217", "Construction Cone Orange", "#F87431", "Sunrise Orange",
				"#E67451", "Mango Orange", "#FF8040", "Dark Orange", "#F88017", "Coral", "#FF7F50",
				"Basket Ball Orange", "#F88158", "Light Salmon", "#F9966B", "Tangerine", "#E78A61", "Dark Salmon",
				"#E18B6B", "Light Coral", "#E77471", "Bean Red", "#F75D59", "Valentine Red", "#E55451",
				"Shocking Orange", "#E55B3C", "Red", "#FF0000", "Scarlet", "#FF2400", "Ruby Red", "#F62217",
				"Ferrari Red", "#F70D1A", "Fire Engine Red", "#F62817", "Lava Red", "#E42217", "Love Red", "#E41B17",
				"Grapefruit", "#DC381F", "Chestnut Red", "#C34A2C", "Cherry Red", "#C24641", "Mahogany", "#C04000",
				"Chilli Pepper", "#C11B17", "Cranberry", "#9F000F", "Red Wine", "#990012", "Burgundy", "#8C001A",
				"Chestnut", "#954535", "Blood Red", "#7E3517", "Sienna", "#8A4117", "Sangria", "#7E3817", "Firebrick",
				"#800517", "Maroon", "#810541", "Plum Pie", "#7D0541", "Velvet Maroon", "#7E354D", "Plum Velvet",
				"#7D0552", "Rosy Finch", "#7F4E52", "Puce", "#7F5A58", "Dull Purple", "#7F525D", "Rosy Brown",
				"#B38481", "Khaki Rose", "#C5908E", "Pink Bow", "#C48189", "Lipstick Pink", "#C48793", "Rose",
				"#E8ADAA", "Rose Gold", "#ECC5C0", "Desert Sand", "#EDC9AF", "Pig Pink", "#FDD7E4", "Cotton Candy",
				"#FCDFFF", "Pink Bubble Gum", "#FFDFDD", "Misty Rose", "#FBBBB9", "Pink", "#FAAFBE", "Light Pink",
				"#FAAFBA", "Flamingo Pink", "#F9A7B0", "Pink Rose", "#E7A1B0", "Pink Daisy", "#E799A3", "Cadillac Pink",
				"#E38AAE", "Carnation Pink", "#F778A1", "Blush Red", "#E56E94", "Hot Pink", "#F660AB",
				"Watermelon Pink", "#FC6C85", "Violet Red", "#F6358A", "Deep Pink", "#F52887", "Pink Cupcake",
				"#E45E9D", "Pink Lemonade", "#E4287C", "Neon Pink", "#F535AA", "Magenta", "#FF00FF",
				"Dimorphotheca Magenta", "#E3319D", "Bright Neon Pink", "#F433FF", "Pale Violet Red", "#D16587",
				"Tulip Pink", "#C25A7C", "Medium Violet Red", "#CA226B", "Rogue Pink", "#C12869", "Burnt Pink",
				"#C12267", "Bashful Pink", "#C25283", "Dark Carnation Pink", "#C12283", "Plum", "#B93B8F",
				"Viola Purple", "#7E587E", "Purple Iris", "#571B7E", "Plum Purple", "#583759", "Indigo", "#4B0082",
				"Purple Monster", "#461B7E", "Purple Haze", "#4E387E", "Eggplant", "#614051", "Grape", "#5E5A80",
				"Purple Jam", "#6A287E", "Dark Orchid", "#7D1B7E", "Purple Flower", "#A74AC7", "Medium Orchid",
				"#B048B5", "Purple Amethyst", "#6C2DC7", "Dark Violet", "#842DCE", "Violet", "#8D38C9",
				"Purple Sage Bush", "#7A5DC7", "Lovely Purple", "#7F38EC", "Purple", "#8E35EF", "Aztech Purple",
				"#893BFF", "Medium Purple", "#8467D7", "Jasmine Purple", "#A23BEC", "Purple Daffodil", "#B041FF",
				"Tyrian Purple", "#C45AEC", "Crocus Purple", "#9172EC", "Purple Mimosa", "#9E7BFF", "Heliotrope Purple",
				"#D462FF", "Crimson", "#E238EC", "Purple Dragon", "#C38EC7", "Lilac", "#C8A2C8", "Blush Pink",
				"#E6A9EC", "Mauve", "#E0B0FF", "Wisteria Purple", "#C6AEC7", "Blossom Pink", "#F9B7FF", "Thistle",
				"#D2B9D3", "Periwinkle", "#E9CFEC", "Lavender Pinocchio", "#EBDDE2", "Lavender blue", "#E3E4FA",
				"Pearl", "#FDEEF4", "SeaShell", "#FFF5EE", "Milk White", "#FEFCFF", "White", "#FFFFFF" };

		for (int i = 0; i < colors.length; i += 2) {
			COLOR_MAP.put(colors[i].toLowerCase(), ColorFromString(colors[i + 1]));
		}
	}

	public static Color ColorFromString(String str) {
		if (str.indexOf("#") >= 0) {
			return new Color(Integer.valueOf(str.substring(1, 3), 16), Integer.valueOf(str.substring(3, 5), 16),
					Integer.valueOf(str.substring(5, 7), 16));
		} else {
			return COLOR_MAP.getOrDefault(str.toLowerCase(), null);
		}
	}

	public static Color getRandomColor() {
		List<String> keys = new ArrayList<String>(COLOR_MAP.keySet());
		String key = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
		return COLOR_MAP.get(key);
	}

	public static final TimerTask WrapTimerTask(Runnable r) {
		return new TimerTask() {
			@Override
			public void run() {
				r.run();
			}
		};
	}

	public static String getCharForNumber(int i) {
		return i > 0 && i < 27 ? String.valueOf((char) (i + 64)) : null;
	}

	public static interface Function<R> {
		R get();
	}
}