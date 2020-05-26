package dk.kb.alma.client.utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class ClasspathUtils {
    public static Path getPathFromClasspath(String s) {
        try {
            return new File(Thread.currentThread().getContextClassLoader().getResource(s).toURI()).toPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String readFileFromClasspath(String name) throws IOException {
        try (InputStream resourceAsStream = Thread.currentThread()
                                                  .getContextClassLoader()
                                                  .getResourceAsStream(name);) {
            if (resourceAsStream == null) {
                AlmaObjectUtils.log.warn("Failed to find file {}, returning null", name);
                return null;
            } else {
                return IOUtils.toString(resourceAsStream, Charset.defaultCharset());
            }
        }
    }
}
