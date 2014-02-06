package edu.gatech.gtri.orafile;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class OrafileRenderer {

    protected static final String LINE_SEPARATOR = "\n";
    // System.getProperty("line.separator");

    private static final String APOSTROPHE = "\"";
    private static final String BRACKET_OPEN = "(";
    private static final String BRACKET_CLOSED = ")";

    final boolean sortByKey;

    public OrafileRenderer() {
        sortByKey = false;
    }

    OrafileRenderer(boolean sortByKey) {
        this.sortByKey = sortByKey;
    }

    /**
     * @param sortByKey
     *            True to sort entries by key. False to preserve original
     *            ordering.
     * @return A new {@link OrafileRenderer}.
     */
    public OrafileRenderer sortByKey(boolean sortByKey) {
        return new OrafileRenderer(sortByKey);
    }

    private enum Parens {

        Yes, No;

        boolean yes() {
            return this == Yes;
        }
    }

    public String renderFile(OrafileDict dict) {
        StringWriter writer = new StringWriter();
        try {
            renderFile(dict, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    public void renderFile(OrafileDict dict, Writer writer) throws IOException {
        Iterator<OrafileDef> defs = defs(dict).iterator();
        while (defs.hasNext()) {
            OrafileDef def = defs.next();

            renderDef(writer, def, Parens.No, "");

            if (defs.hasNext()) {
                writer.append(LINE_SEPARATOR);
            }
        }
    }

    void renderDef(Writer writer, final OrafileDef def, final Parens parens, final String indent) throws IOException {
        writer.append(indent);
        if (parens.yes()) {
            writer.append(BRACKET_OPEN);
        }

        renderVal(writer, def.getName(), def.getVal(), parens, indent);

        if (parens.yes()) {
            writer.append(BRACKET_CLOSED).append(LINE_SEPARATOR);
        }
    }

    void renderVal(Writer writer, final String defName, final OrafileVal val, final Parens parens, final String indent)
            throws IOException {
        final String nextIndent = indent + "  ";

        if (val instanceof OrafileString) {
            String stringVal = ((OrafileString) val).toString();

            writer.append(defName).append(" = ");
            renderString(writer, stringVal);
        } else if (val instanceof OrafileStringList) {
            OrafileStringList stringListVal = (OrafileStringList) val;

            writer.append(defName).append(" = ").append(BRACKET_OPEN).append(LINE_SEPARATOR);
            Iterator<String> stringVals = stringListVal.asStringList().iterator();
            while (stringVals.hasNext()) {
                String stringVal = stringVals.next();

                writer.append(nextIndent);
                renderString(writer, stringVal);
                if (stringVals.hasNext()) {
                    writer.append(",");
                }
                writer.append(LINE_SEPARATOR);
            }
            writer.append(indent).append(BRACKET_CLOSED);
        } else {
            // val is instance of OrafileDict
            OrafileDict dict = (OrafileDict) val;

            writer.append(defName).append(" =").append(LINE_SEPARATOR);

            for (OrafileDef nextDef : defs(dict)) {
                renderDef(writer, nextDef, Parens.Yes, nextIndent);
            }

            if (parens.yes()) {
                writer.append(indent);
            }
        }
    }

    static final Pattern SAFE_STRING = Pattern.compile("^[A-Za-z0-9\\Q<>/.:;-_$+*&!%?@\\E]+$");

    void renderString(Writer writer, String string) throws IOException {
        if (SAFE_STRING.matcher(string).matches()) {
            writer.append(string);
        } else {
            String escaped = string.replaceAll("\\\\", "\\\\\\\\").replaceAll(APOSTROPHE, "\\\\\"");
            writer.append(APOSTROPHE).append(escaped).append(APOSTROPHE);
        }
    }

    List<OrafileDef> defs(OrafileDict dict) {
        if (!this.sortByKey) {
            return dict.asList();
        }
        List<OrafileDef> defs = new ArrayList<OrafileDef>(dict.asList());
        Collections.sort(defs, DEF_KEY_COMPARATOR);
        return defs;
    }

    static final Comparator<OrafileDef> DEF_KEY_COMPARATOR = new Comparator<OrafileDef>() {
        @Override
        public int compare(OrafileDef a, OrafileDef b) {
            return a.getName().compareTo(b.getName());
        }
    };

}
