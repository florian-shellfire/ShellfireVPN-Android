package de.shellfire.vpn.android;

public class Util {

	public static int getServerTypeResId(ServerType serverType) {
		
		switch (serverType) {
		case Free:
			default:
			return R.string.default_product_type;
		case Premium:
			return R.string.premium;
		case PremiumPlus:
			return R.string.premiumplus;

		}

	}
}
