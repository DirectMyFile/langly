package org.langly

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration

import java.util.regex.Pattern

@CompileStatic
class Langly {
    static Map<String, Object> metadata

    static Pattern vendorPattern

    static {
        loadMetadata()
    }

    static List<Language> languages

    private static void loadMetadata() {
        def cc = new CompilerConfiguration()
        cc.scriptBaseClass = DelegatingScript.class.name
        def shell = new GroovyShell(cc)
        def script = shell.parse(Langly.classLoader.getResourceAsStream("metadata.groovy").newReader() as Reader) as DelegatingScript
        def meta = [:]
        script.setDelegate(meta)
        script.run()
        metadata = meta
        languages = ({ ->
	        def l = []
            metadata.languages.each { Map it ->
		        l << new Language(it)
            }
            l
        })()
        vendorPattern = Pattern.compile(vendor().join("|"))
    }

    static boolean isLanguage(Language lang, FileBlob file) {
        for (String ext in lang.extensions) {
            if (file.name().endsWith(ext)) {
                return true
            }
        }

        for (String filename in lang.filenames) {
            if (filename == file.name()) {
                return true
            }
        }

        def reader = file.code()

        def firstLine = reader.readLine()

        reader.close()

        /* First Line is a Shebang, will parse it */
        if (firstLine != null && firstLine.startsWith("#!")) {
            /* Find Interpreter and Compare */
            def interpreter = Utils.parseShebang(firstLine)
            if (interpreter && interpreter in lang.interpreters) {
                return true
            }
        }

        return false
    }

    static Language detect(FileBlob file) {
        for (language in languages) {
            if (isLanguage(language, file)) {
                return language
            }
        }
        return null
    }

    static Language language(String name) {
        for (language in languages) {
            if (language.name == name) {
                return language
            }
        }
        return null
    }

    static List<String> vendor() {
        metadata.vendor as List<String>
    }

    static List<String> binaryExtensions() {
        metadata.binary_extensions as List<String>
    }

    static boolean isVendorFile(FileBlob file) {
        file.path() ==~ vendorPattern
    }
}
