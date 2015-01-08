package messages;

public enum EnumJoin {OK (0), KO_SERVER_FULL (1), KO_BAD_USER (2), KO_BAD_PASS (3);
private int eJoin;

EnumJoin(int p) {eJoin = p;}
public int showEnumJoin() {return eJoin;}
public int showEnumJoin(EnumJoin eJ)
{
	return eJ.ordinal();
}


}
