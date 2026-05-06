package com.fbp.engine.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addJar(URL url) {
        super.addURL(url);
    }
}
