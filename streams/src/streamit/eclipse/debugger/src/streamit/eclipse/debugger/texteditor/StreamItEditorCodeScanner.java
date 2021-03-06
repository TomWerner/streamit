package streamit.eclipse.debugger.texteditor;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.JavaWordDetector;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.util.PropertyChangeEvent;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * A StreamIt code scanner.
 * 
 * @author kkuo
 */
public final class StreamItEditorCodeScanner extends AbstractJavaScanner {
    
    private static class VersionedWordRule extends WordRule {

        private final IToken fDefaultToken;
        private final String fVersion;
        private final boolean fEnable;
        
        private String fCurrentVersion;

        public VersionedWordRule(IWordDetector detector, IToken defaultToken, String version, boolean enable, String currentVersion) {
            super(detector);

            fDefaultToken= defaultToken;
            fVersion= version;
            fEnable= enable;
            fCurrentVersion= currentVersion;
        }
        
        public void setCurrentVersion(String version) {
            fCurrentVersion= version;
        }
    
        /*
         * @see IRule#evaluate
         */
        public IToken evaluate(ICharacterScanner scanner) {
            IToken token= super.evaluate(scanner);

            if (fEnable) {
                if (fCurrentVersion.equals(fVersion) || token.isUndefined())
                    return token;
                //
                return fDefaultToken;

            } else {
                if (fCurrentVersion.equals(fVersion))
                    return Token.UNDEFINED;
                    
                return token;
            }
        }
    }
    
    private static final String SOURCE_VERSION= JavaCore.COMPILER_SOURCE;
    
    private static String[] fgKeywords= { 
        "abstract", //$NON-NLS-1$
        "break", //$NON-NLS-1$
        "case", "catch", "class", "const", "continue", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "default", "do", //$NON-NLS-2$ //$NON-NLS-1$
        "else", "extends", //$NON-NLS-2$ //$NON-NLS-1$
        "final", "finally", "for", //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "goto", //$NON-NLS-1$
        "if", "implements", "import", "instanceof", "interface", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "native", "new", //$NON-NLS-2$ //$NON-NLS-1$
        "package", "private", "protected", "public", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "return", //$NON-NLS-1$
        "static", "super", "switch", "synchronized", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "this", "throw", "throws", "transient", "try", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
        "volatile", //$NON-NLS-1$
        "while" //$NON-NLS-1$
    };
    
    private static String[] fgNewKeywords= { "assert" }; //$NON-NLS-1$
    
    private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "strictfp", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
    
    private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

    private static String[] fgTokenProperties= {
        IJavaColorConstants.JAVA_KEYWORD,
        IJavaColorConstants.JAVA_STRING,
        IJavaColorConstants.JAVA_DEFAULT
    };
    
    private VersionedWordRule fVersionedWordRule;

    // CHANGED kkuo
    private static List lKeywords;
    private static List lStrKeywords;
    // added StreamIt's bit, complex,
    private static List lTypes;
    private static List lConstants; 
    private static List lStrCommon;
    private static StreamItColorProvider fProvider;
    
    /**
     * Creates a Java code scanner
     */
    public StreamItEditorCodeScanner(IColorManager manager, IPreferenceStore store) {
        super(manager, store);
        fProvider = new StreamItColorProvider();
        initialize();
    }
    
    /*
     * @see AbstractJavaScanner#getTokenProperties()
     */
    protected String[] getTokenProperties() {
        return fgTokenProperties;
    }

    /*
     * @see AbstractJavaScanner#createRules()
     */
    protected List createRules() {
        
        List rules = new ArrayList();

        /*
        // Add rule for character constants.
        Token token= getToken(IJavaColorConstants.JAVA_STRING);
        rules.add(new SingleLineRule("'", "'", token, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
        
        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
        
        // Add word rule for new keywords, 4077
        Object version= null;
        try {
        version= JavaCore.getOptions().get(SOURCE_VERSION);
        } catch (NullPointerException x) {
        // plugin not initialized - happens in test code
        }
        
        if (version instanceof String) {
        token= getToken(IJavaColorConstants.JAVA_DEFAULT);
        fVersionedWordRule= new VersionedWordRule(new JavaWordDetector(), token, "1.4", true, (String) version); //$NON-NLS-1$
            
        token= getToken(IJavaColorConstants.JAVA_KEYWORD);
        for (int i=0; i<fgNewKeywords.length; i++)
        fVersionedWordRule.addWord(fgNewKeywords[i], token);

        rules.add(fVersionedWordRule);
        }

        // Add word rule for keywords, types, and constants.
        token= getToken(IJavaColorConstants.JAVA_DEFAULT);
        WordRule wordRule= new WordRule(new JavaWordDetector(), token);
        
        token= getToken(IJavaColorConstants.JAVA_KEYWORD);

        initializeCategory();
        for (int i = 0; i < lKeywords.size(); i++)
        wordRule.addWord((String) lKeywords.get(i), token);
        // kkuo - added word rules for StreamIt keywords
        for (int i = 0; i < lStrKeywords.size(); i++)
        wordRule.addWord((String) lStrKeywords.get(i), token);
        for (int i = 0; i < lTypes.size(); i++)
        wordRule.addWord((String) lTypes.get(i), token);
        for (int i = 0; i < lConstants.size(); i++)
        wordRule.addWord((String) lConstants.get(i), token);
        // kkuo - added word rules for StreamIt's commonly used words
        for (int i = 0; i < lStrCommon.size(); i++)
        wordRule.addWord((String) lStrCommon.get(i), token);
        rules.add(wordRule);
        */

        IToken keyword = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.KEYWORD)));
        IToken strKeyword = new
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.
                                             STRKEYWORD)));
        IToken type = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.TYPE)));
        IToken string = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.STRING)));
        IToken comment = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.
                                             SINGLE_LINE_COMMENT)));
        IToken strCommon = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.
                                             STRCOMMON)));
        IToken other = new 
            Token(new TextAttribute(fProvider.
                                    getColor(StreamItColorProvider.DEFAULT)));
                
        // Add rule for single line comments.
        rules.add(new EndOfLineRule(IStreamItEditorConstants.COMMENTS, comment)); //$NON-NLS-1$
        
        // Add rule for strings and character constants.
        rules.add(new SingleLineRule(IStreamItEditorConstants.DOUBLE_QUOTE_STRING, IStreamItEditorConstants.DOUBLE_QUOTE_STRING, string, IStreamItEditorConstants.BACKSLASH_STRING)); 
        //$NON-NLS-2$ //$NON-NLS-1$
        
        rules.add(new SingleLineRule(IStreamItEditorConstants.QUOTE_STRING, IStreamItEditorConstants.QUOTE_STRING, string, IStreamItEditorConstants.BACKSLASH_STRING)); //$NON-NLS-2$ 
        //$NON-NLS-1$
        
        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
        
        // Add word rule for new keywords, 4077
        Object version= null;
        try {
            version= JavaCore.getOptions().get(SOURCE_VERSION);
        } catch (NullPointerException x) {
            // plugin not initialized - happens in test code
        }

        Token token;
        if (version instanceof String) {
            token= getToken(IJavaColorConstants.JAVA_DEFAULT);
            fVersionedWordRule= new VersionedWordRule(new JavaWordDetector(), token, IStreamItEditorConstants.JAVA_VERSION, true, (String) version); //$NON-NLS-1$
            
            token= getToken(IJavaColorConstants.JAVA_KEYWORD);
            for (int i=0; i<fgNewKeywords.length; i++)
                fVersionedWordRule.addWord(fgNewKeywords[i], token);

            rules.add(fVersionedWordRule);
        }

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new JavaWordDetector(), other);
        initializeCategory();
        for (int i = 0; i < lKeywords.size(); i++)
            wordRule.addWord((String) lKeywords.get(i), keyword);
        // kkuo - added word rules for StreamIt keywords
        for (int i = 0; i < lStrKeywords.size(); i++)
            wordRule.addWord((String) lStrKeywords.get(i), strKeyword);
        for (int i = 0; i < lTypes.size(); i++)
            wordRule.addWord((String) lTypes.get(i), type);
        for (int i = 0; i < lConstants.size(); i++)
            wordRule.addWord((String) lConstants.get(i), type);
        // kkuo - added word rules for StreamIt's commonly used words
        for (int i = 0; i < lStrCommon.size(); i++)
            wordRule.addWord((String) lStrCommon.get(i), strCommon);
        rules.add(wordRule);
        
        setDefaultReturnToken(getToken(IJavaColorConstants.JAVA_DEFAULT));
        return rules;
    }

    // CHANGED kkuo 
    private void initializeCategory() {
        if (StreamItDebuggerPlugin.getDefault() == null) {
            for (int i = 0; i < fgKeywords.length; i++)
                lKeywords.add(fgKeywords[i]);
            
            for (int i = 0; 
                 i < IStreamItEditorConstants.FG_STR_KEYWORD_DEFAULTS.length; i++)
                lStrKeywords.add(IStreamItEditorConstants.FG_STR_KEYWORD_DEFAULTS[i]);          
            for (int i = 0; i < fgTypes.length; i++)
                lTypes.add(fgTypes[i]);
            for (int i = 0; i < IStreamItEditorConstants.FG_STR_TYPE_DEFAULTS.length;
                 i++)
                lTypes.add(IStreamItEditorConstants.FG_STR_TYPE_DEFAULTS[i]);
            for (int i = 0; i < fgConstants.length; i++)
                lConstants.add(fgConstants[i]);
            for (int i = 0; 
                 i < IStreamItEditorConstants.FG_STR_COMMON_DEFAULTS.length; i++)
                lStrCommon.add(IStreamItEditorConstants.FG_STR_COMMON_DEFAULTS[i]);
        } else {
            IPreferenceStore store 
                = StreamItDebuggerPlugin.getDefault().getPreferenceStore();
            StringTokenizer strtok;
            String toUse, temp;
            String[] toIter;
            List toAdd;
            
            for (int i = 0; i < 5; i++) {
                switch (i) {
                case 0: 
                    toUse = IStreamItEditorConstants.PRE_KEYWORD;
                    toIter = fgKeywords;
                    break;
                case 1:
                    toUse = IStreamItEditorConstants.PRE_STR_KEYWORD;
                    toIter = IStreamItEditorConstants.FG_STR_KEYWORD_DEFAULTS;
                    break;
                case 2:
                    toUse = IStreamItEditorConstants.PRE_TYPE;
                    toIter = fgTypes; 
                    break;
                case 3: 
                    toUse = IStreamItEditorConstants.PRE_CONSTANT;
                    toIter = fgConstants; 
                    break;
                default: 
                    toUse = IStreamItEditorConstants.PRE_STR_COMMON;
                    toIter = IStreamItEditorConstants.FG_STR_COMMON_DEFAULTS;
                    break;
                }
                temp = store.getString(toUse);
                toAdd = new Vector();
                if (temp.equals(IPreferenceStore.STRING_DEFAULT_DEFAULT)) {
                    for (int j = 0; j < toIter.length; j++)
                        toAdd.add(toIter[j]);
                    if (toUse.equals(IStreamItEditorConstants.PRE_TYPE)) {
                        toIter = IStreamItEditorConstants.FG_STR_TYPE_DEFAULTS;
                        for (int j = 0; j < toIter.length; j++)
                            toAdd.add(toIter[j]);
                    }
                } else {
                    strtok = new StringTokenizer(temp, IStreamItEditorConstants.NEWLINE_STRING);                
                    while (strtok.hasMoreTokens())
                        toAdd.add(strtok.nextToken());
                }
                switch (i) {
                case 0: lKeywords = toAdd; break;
                case 1: lStrKeywords = toAdd; break;
                case 2: lTypes = toAdd; break;
                case 3: lConstants = toAdd; break;
                default: lStrCommon = toAdd; break;
                }
            }
        }
    }

    public static String[] getFGKeywords() {
        return fgKeywords;
    }
    public static String[] getFGTypes() {
        return fgTypes;
    }
    public static String[] getFGConstants() {
        return fgConstants;
    }
    // end CHANGED kkuo

    /*
     * @see RuleBasedScanner#setRules(IRule[])
     */
    public void setRules(IRule[] rules) {
        int i;
        for (i= 0; i < rules.length; i++)
            if (rules[i].equals(fVersionedWordRule))
                break;

        // not found - invalidate fVersionedWordRule
        if (i == rules.length)
            fVersionedWordRule= null;
        
        super.setRules(rules);  
    }

    /*
     * @see AbstractJavaScanner#affectsBehavior(PropertyChangeEvent)
     */ 
    public boolean affectsBehavior(PropertyChangeEvent event) {
        return event.getProperty().equals(SOURCE_VERSION) || super.affectsBehavior(event);
    }

    /*
     * @see AbstractJavaScanner#adaptToPreferenceChange(PropertyChangeEvent)
     */
    public void adaptToPreferenceChange(PropertyChangeEvent event) {
        
        if (event.getProperty().equals(SOURCE_VERSION)) {
            Object value= event.getNewValue();

            if (value instanceof String) {
                String s= (String) value;
    
                if (fVersionedWordRule != null)
                    fVersionedWordRule.setCurrentVersion(s);            
            }
            
        } else if (super.affectsBehavior(event)) {
            super.adaptToPreferenceChange(event);
        }
    }
}
