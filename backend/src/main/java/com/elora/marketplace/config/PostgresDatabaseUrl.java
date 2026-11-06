package com.elora.marketplace.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

record PostgresDatabaseUrl(String jdbcUrl, String username, String password) {

    static PostgresDatabaseUrl parse(String value) {
        String url = value == null ? "" : value.trim();
        if (url.startsWith("jdbc:postgresql://") || url.startsWith("jdbc:h2:")) {
            return new PostgresDatabaseUrl(url, null, null);
        }

        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("DB_URL must be a PostgreSQL connection URI or JDBC URL.", exception);
        }

        String scheme = uri.getScheme();
        if (!"postgres".equalsIgnoreCase(scheme) && !"postgresql".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("DB_URL must use postgres://, postgresql://, or jdbc:postgresql://.");
        }
        if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() < 2) {
            throw new IllegalArgumentException("DB_URL must include a PostgreSQL host and database name.");
        }
        if (uri.getRawFragment() != null) {
            throw new IllegalArgumentException("DB_URL must not include a URI fragment.");
        }

        String authority = uri.getRawAuthority();
        int userInfoEnd = authority.lastIndexOf('@');
        String jdbcAuthority = userInfoEnd >= 0 ? authority.substring(userInfoEnd + 1) : authority;
        String jdbcUrl = "jdbc:postgresql://" + jdbcAuthority + uri.getRawPath();
        if (uri.getRawQuery() != null) {
            jdbcUrl += "?" + uri.getRawQuery();
        }

        if (userInfoEnd < 0) {
            return new PostgresDatabaseUrl(jdbcUrl, null, null);
        }

        String[] credentials = authority.substring(0, userInfoEnd).split(":", 2);
        String username = decode(credentials[0]);
        String password = credentials.length == 2 ? decode(credentials[1]) : null;
        return new PostgresDatabaseUrl(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
