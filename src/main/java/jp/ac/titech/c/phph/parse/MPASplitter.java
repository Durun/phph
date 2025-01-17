package jp.ac.titech.c.phph.parse;

import jp.ac.titech.c.phph.model.Range;
import jp.ac.titech.c.phph.model.Statement;
import lombok.extern.slf4j.Slf4j;
import yoshikihigo.cpanalyzer.CPAConfig;
import yoshikihigo.cpanalyzer.LANGUAGE;
import yoshikihigo.cpanalyzer.StringUtility;
import yoshikihigo.cpanalyzer.lexer.token.IMPORT;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The splitter used by MPAnalyzer.
 */
@Slf4j
public class MPASplitter implements Splitter {

    public static final Pattern RE_IDENT = Pattern.compile("^[a-zA-Z_].*|.* [a-zA-Z_].*");

    public static final Pattern RE_NON_ESSENTIAL_ASSIGNMENT =
            Pattern.compile("^(\\w+( < \\w+ >)? |this \\. )?\\$V0 = (\"\\$L\"|\\$L|\\$V0|\\$V1|null) ;$");

    static {
        CPAConfig.initialize(new String[]{"-n"}); // normalize
    }

    @Override
    public String targetExtension() {
        return "java";
    }

    @Override
    public List<Statement> split(String source) {
        return StringUtility.splitToStatements(source, LANGUAGE.JAVA)
                .stream()
                .filter(s -> !(s.tokens.get(0) instanceof IMPORT)) // drop import statements
                .filter(this::isEssential) // drop non-essential statements
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private boolean isEssential(final yoshikihigo.cpanalyzer.data.Statement s) {
        final boolean result = RE_IDENT.matcher(s.nText).matches() &&
                !RE_NON_ESSENTIAL_ASSIGNMENT.matcher(s.nText).matches();
        if (!result) {
            log.trace("Filtering non-essential statement: {}", s.nText);
        }
        return result;
    }

    private Statement convert(final yoshikihigo.cpanalyzer.data.Statement s) {
        return Statement.of(s.rText, s.nText, Range.of(s.fromLine, s.toLine + 1));
    }
}
