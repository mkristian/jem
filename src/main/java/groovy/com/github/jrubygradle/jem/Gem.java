package com.github.jrubygradle.jem;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Plain Old Java Object for an enumeration of metadata provided by a gem
 */
public class Gem {
    @JsonProperty
    public String name;

    @JsonProperty
    public Version version;

    @JsonProperty
    public String description;

    @JsonProperty
    public String platform;

    @JsonProperty
    public Object email;

    @JsonProperty
    public String homepage;

    @JsonProperty
    public List<String> authors;

    @JsonProperty
    public List<String> files;

    @JsonProperty(value="test_files")
    public List<String> testFiles;

    @JsonProperty
    public List<String> executables;

    @JsonProperty
    public String bindir;

    @JsonProperty(value="require_paths")
    public List<String> requirePaths;

    @JsonProperty
    public List<String> licenses;

    @JsonProperty(value="specification_version")
    public Integer specificationVersion;

    @JsonProperty(value="rubygems_version")
    public String rubygemsVersion;

    /**
     * Take the given argument and produce a {@code Gem} instance
     *
     * @param metadata a {@code java.lang.String}, a {@code java.io.File} or a {@code java.util.zip.GZIPInputStream}
     * @return
     */
    public static Gem fromFile(Object metadata) throws JsonProcessingException, IOException {
        if (metadata instanceof String) {
            return createGemFromFile(new File((String)metadata));
        }
        if (metadata instanceof File) {
            return createGemFromFile((File)(metadata));
        }
        if (metadata instanceof InputStream) {
            return createGemFromInputStream((InputStream)(metadata));
        }

        return null;
    }

    /**
     * Output the gemspec stub for this file
     *
     * See <https://github.com/rubygems/rubygems/blob/165030689defe16680b7f336232db62024f49de4/lib/rubygems/specification.rb#L2422-L2512>
     *
     * @return
     */
    public String toRuby() throws JsonProcessingException {
        String[] specification = {
                "# -*- encoding: utf-8 -*-",
                "#",
                String.format("# stub: %s %s %s %s",
                        name, version.version, platform, join(requirePaths.toArray(new String[0]), "\0")),
                "#",
                "# NOTE: This specification was generated by `jem`",
                "#  <https://github.com/jruby-gradle/jem>",
                "",
                "Gem::Specification.new do |s|",
                "  s.name = " + sanitize(name),
                "  s.version = " + sanitize(version.version),
                "  s.description = " + sanitize(description),
                "  s.homepage = " + sanitize(homepage),
                "  s.authors = " + sanitize(authors),
                "  s.email = " + sanitize(email),
                "  s.licenses = " + sanitize(licenses),
                "",
                "  s.platform = " + sanitize(platform),
                "  s.require_paths = " + sanitize(requirePaths),
                "  s.executables = " + sanitize(executables),
                "  s.rubygems_version = " + sanitize(rubygemsVersion),
            "end",
        };
        return join(specification);
    }

    private String join(String[] segments) {
        return this.join(segments, System.getProperty("line.separator"));
    }

    private String join(String[] segments, String split) {
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            builder.append(String.format("%s%s", segment, split));
        }
        return builder.toString();
    }

    /** Convert whatever object we're given into a safe (see: JSON) reprepsentation */
    protected String sanitize(Object value) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(value);
    }

    private static Gem createGemFromFile(File gemMetadataFile) throws
            JsonProcessingException, IOException {
        if (!gemMetadataFile.exists()) {
            return null;
        }
        return getYamlMapper().readValue(gemMetadataFile, Gem.class);
    }

    private static Gem createGemFromInputStream(InputStream gemMetadataStream) throws
            JsonProcessingException, IOException {
        return getYamlMapper().readValue(gemMetadataStream, Gem.class);
    }

    private static ObjectMapper getYamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
