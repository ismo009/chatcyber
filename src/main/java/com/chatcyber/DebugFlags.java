package com.chatcyber;

public final class DebugFlags {
    //Pour debogger les pb de clés privées qui marche pas sous RSA

    private DebugFlags() {}

    public static final boolean EXPOSE_IBE_PRIVATE_KEY = false; //Afficher clée privée en clair dans logs + UI
}
