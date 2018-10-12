/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.layout;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Appends a series of JSON events as strings serialized as bytes.
 *
 * <h3>Complete well-formed JSON vs. fragment JSON</h3>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed JSON document. By default, with
 * {@code complete="false"}, you should include the output as an <em>external file</em> in a separate file to form a
 * well-formed JSON document.
 * </p>
 * <p>
 * If {@code complete="false"}, the appender does not write the JSON open array character "[" at the start
 * of the document, "]" and the end, nor comma "," between records.
 * </p>
 * <h3>Encoding</h3>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h3>Pretty vs. compact JSON</h3>
 * <p>
 * By default, the JSON layout is not compact (a.k.a. "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the text. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, escaped end-of-lines.
 * </p>
 * <h3>Additional Fields</h3>
 * <p>
 * This property allows addition of custom fields into generated JSON.
 * {@code <JsonLayout><KeyValuePair key="foo" value="bar"/></JsonLayout>} inserts {@code "foo":"bar"} directly
 * into JSON output. Supports Lookup expressions.
 * </p>
 */
@Plugin(name = "LogstashJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class LogstashJsonLayout extends AbstractJacksonLayout {

    private static final String DEFAULT_FOOTER = "]";

    private static final String DEFAULT_HEADER = "[";

    static final String CONTENT_TYPE = "application/json";

    private ObjectMapper objectMapper;

    public static class Builder<B extends Builder<B>> extends AbstractJacksonLayout.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<LogstashJsonLayout> {

        @PluginBuilderAttribute
        private boolean propertiesAsList;

        @PluginBuilderAttribute
        private boolean objectMessageAsJsonObject;

        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields;

        public Builder() {
            super();
            setCharset(StandardCharsets.UTF_8);
        }

        @Override
        public LogstashJsonLayout build() {
            final boolean encodeThreadContextAsList = isProperties() && propertiesAsList;
            final String headerPattern = toStringOrNull(getHeader());
            final String footerPattern = toStringOrNull(getFooter());
            return new LogstashJsonLayout(getConfiguration(), isLocationInfo(), isProperties(), encodeThreadContextAsList,
                    isComplete(), isCompact(), getEventEol(), headerPattern, footerPattern, getCharset(),
                    isIncludeStacktrace(), isStacktraceAsString(), isIncludeNullDelimiter(),
                    getAdditionalFields(), getObjectMessageAsJsonObject());
        }

        public boolean isPropertiesAsList() {
            return propertiesAsList;
        }

        public B setPropertiesAsList(final boolean propertiesAsList) {
            this.propertiesAsList = propertiesAsList;
            return asBuilder();
        }

        public boolean getObjectMessageAsJsonObject() {
            return objectMessageAsJsonObject;
        }

        public B setObjectMessageAsJsonObject(final boolean objectMessageAsJsonObject) {
            this.objectMessageAsJsonObject = objectMessageAsJsonObject;
            return asBuilder();
        }

        @Override
        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        @Override
        public B setAdditionalFields(KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields;
            return asBuilder();
        }
    }

    private LogstashJsonLayout(final Configuration config, final boolean locationInfo, final boolean properties,
                               final boolean encodeThreadContextAsList,
                               final boolean complete, final boolean compact, final boolean eventEol,
                               final String headerPattern, final String footerPattern, final Charset charset,
                               final boolean includeStacktrace, final boolean stacktraceAsString,
                               final boolean includeNullDelimiter,
                               final KeyValuePair[] additionalFields, final boolean objectMessageAsJsonObject) {
        super(config, new JacksonFactory.JSON(encodeThreadContextAsList, includeStacktrace, stacktraceAsString, objectMessageAsJsonObject).newWriter(
                locationInfo, properties, compact),
                charset, compact, complete, eventEol,
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(headerPattern).setDefaultPattern(DEFAULT_HEADER).build(),
                PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(footerPattern).setDefaultPattern(DEFAULT_FOOTER).build(),
                includeNullDelimiter,
                additionalFields);
        this.objectMapper = new Log4jJsonObjectMapper(encodeThreadContextAsList, includeStacktrace, stacktraceAsString, objectMessageAsJsonObject);
    }

    /**
     * Returns appropriate JSON header.
     *
     * @return a byte array containing the header, opening the JSON array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final String str = serializeToString(getHeaderSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    /**
     * Returns appropriate JSON footer.
     *
     * @return a byte array containing the footer, closing the JSON array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(this.eol);
        final String str = serializeToString(getFooterSerializer());
        if (str != null) {
            buf.append(str);
        }
        buf.append(this.eol);
        return getBytes(buf.toString());
    }

    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("version", "2.0");
        return result;
    }

    /**
     * @return The content type.
     */
    @Override
    public String getContentType() {
        return CONTENT_TYPE + "; charset=" + this.getCharset();
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Creates a JSON Layout using the default settings. Useful for testing.
     *
     * @return A JSON Layout.
     */
    public static LogstashJsonLayout createDefaultLayout() {
        return new LogstashJsonLayout(new DefaultConfiguration(), false, false, false, false, false, false,
                DEFAULT_HEADER, DEFAULT_FOOTER, StandardCharsets.UTF_8, true, false, false, null, false);
    }

    @Override
    public Object wrapLogEvent(final LogEvent event) {
        // Construct map for serialization - note that we are intentionally using original LogEvent
        Map<String, Object> additionalFieldsMap = resolveAdditionalFields(event);
        // This class combines LogEvent with AdditionalFields during serialization
        return new LogEventWithAdditionalFields(event, additionalFieldsMap);
    }

    @Override
    public void toSerializable(final LogEvent event, final Writer writer) throws IOException {
        if (complete && eventCount > 0) {
            writer.append(", ");
        }
        super.toSerializable(event, writer);
    }

    private Map<String, Object> resolveAdditionalFields(LogEvent logEvent) {
        // Note: LinkedHashMap retains order
        final Map<String, Object> additionalFieldsMap = new LinkedHashMap<>(additionalFields.length);
        final StrSubstitutor strSubstitutor = configuration.getStrSubstitutor();

        // Go over each field
        for (ResolvableKeyValuePair pair : additionalFields) {
            if (pair.valueNeedsLookup) {
                // Resolve value
                additionalFieldsMap.put(pair.key, strSubstitutor.replace(logEvent, pair.value));
            } else {
                // Plain text value
                additionalFieldsMap.put(pair.key, pair.value);
            }
        }

        // Go over MDC, unescape json
        logEvent.getContextData().forEach((key, value) -> {
            if (isJson(value.toString())) {
                try {
                    JsonNode node = objectMapper.readTree(value.toString());
                    additionalFieldsMap.put(key, node);
                } catch (IOException e) {
                    additionalFieldsMap.put(key, value);
                }
            } else {
                additionalFieldsMap.put(key, value);
            }
        });

        return additionalFieldsMap;
    }

    private boolean isJson(String value) {
        if (value.matches("^\\[(.*)]$") || value.matches("^\\{.*}$")) {
            return true;
        } else {
            return false;
        }
    }

    @JsonRootName(XmlConstants.ELT_EVENT)
    @JacksonXmlRootElement(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_EVENT)
    public static class LogEventWithAdditionalFields {

        private final LogEvent logEvent;
        private final Map<String, Object> additionalFields;

        static final String LOG_STASH_ISO8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
        static final DateFormat iso8601DateFormat = new SimpleDateFormat(LOG_STASH_ISO8601_TIMESTAMP_FORMAT);

        public LogEventWithAdditionalFields(LogEvent logEvent, Map<String, Object> additionalFields) {
            this.logEvent = logEvent;
            this.additionalFields = additionalFields;
        }

        @JsonUnwrapped
        public Object getLogEvent() {
            return logEvent;
        }

        @JsonAnyGetter
        @SuppressWarnings("unused")
        public Map<String, Object> getAdditionalFields() {
            return additionalFields;
        }

        @JsonGetter("@version")
        @SuppressWarnings("unused")
        public String getVersion() {
            return "1"; //LOGSTASH VERSION
        }

        @JsonGetter("@timestamp")
        @SuppressWarnings("unused")
        public String getTimestamp() {
            return iso8601DateFormat.format(new Date(logEvent.getTimeMillis()));
        }

    }

}
