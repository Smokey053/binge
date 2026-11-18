package com.binge.data;

import com.binge.model.Catalog;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Reads the movie catalog ships with the application. */
public final class CatalogRepository {

    /** Where the catalog lives on the classpath */
    public static final String CATALOG_RESOURCE = "/com/binge/catalog.json";

    private CatalogRepository() {
    }

    public static Catalog load() {
        try (InputStream in = CatalogRepository.class.getResourceAsStream(CATALOG_RESOURCE)) {
            if (in == null) {
                throw new IOException("Missing resource " + CATALOG_RESOURCE);
            }
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(in, Catalog.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the movie catalog", e);
        }
    }
}
