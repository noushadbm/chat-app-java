package com.chatapp.client;

/**
 * Native package entry point.
 *
 * Launching a class that directly extends JavaFX Application can make the
 * Java launcher require JavaFX modules on the module path. This wrapper keeps
 * packaged classpath launches working with the bundled JavaFX jars.
 */
public class ChatClientLauncher {

    public static void main(String[] args) {
        ChatClientApplication.main(args);
    }
}
