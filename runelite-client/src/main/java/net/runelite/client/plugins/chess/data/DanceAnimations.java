package net.runelite.client.plugins.chess.data;

public enum DanceAnimations {
	CHEER(862),
    DANCE(866),
    JIG(2106),
    SPIN(2107),
    HEADBANG(2108),
    ZOMBIE_DANCE(3543),
    SMOOTH_DANCE(7533),
    CRAZY_DANCE(7537),
    JUMP_FOR_JOY(2109),
    CHICKEN_DANCE(1835),
    GOBLIN_SALUTE(2128);
	
	public int id;

	DanceAnimations(int id){
		this.id = id;
	}
}
