package lint;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import burp.BurpExtender;
import utils.StringUtils;
import static utils.Constants.EMPTY_STRING;
import static burp.BurpExtender.log;

/**
 * Beautify
 */
public class Beautify {

    private static Scriptable scope = null;
    private static Function beautifyFunction = null;

    // Good example
    // https://github.com/mozilla/rhino/blob/master/examples/DynamicScopes.java

    public Beautify() throws BeautifyNotFound {
        try {
            Context cx = Context.enter();
            // cx.setOptimizationLevel(-1);
            scope = cx.initSafeStandardObjects();

            // Load beautify.js
            String jsbeautifyFile = utils.Resources.getResourceFile(BurpExtender.class, "/beautify.js");
            // Evaluate it.+
            cx.evaluateString(scope, jsbeautifyFile, "beautify.js", 0, null);
            // Try to get the function.
            Object beautifyFunctionObject = scope.get("beautify", scope);
            if (!(beautifyFunctionObject instanceof Function)) {
                throw new BeautifyNotFound(
                        "beautify is undefined or not a function, it's " + beautifyFunctionObject.toString());
            } else {
                beautifyFunction = (Function) beautifyFunctionObject;
            }
        } catch (Exception e) {
            StringUtils.printStackTrace(e);
        } finally {
            Context.exit();
        }
        // If beautify is null, the constructor has failed.
    }

    public String beautify(String minJS) {
        // Following this tutorial
        // https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Embedding_tutorial

        String result = EMPTY_STRING;

        if (StringUtils.isEmpty(minJS))
            return result;

        try {
            Context cx = Context.enter();
            // Pass the minified JavaScript.
            Object functionArgs[] = { minJS };
            Object rst = beautifyFunction.call(cx, scope, scope, functionArgs);
            result = Context.toString(rst);
            if (StringUtils.isEmpty(result)) result = EMPTY_STRING;
        } catch (Exception e) {
            // TODO Throw an exception here instead?
            StringUtils.printStackTrace(e);
        } finally {
            Context.exit();
        }
        return result;
    }

    public void beautifyFile(String inFilePath, String outFilePath) throws IOException {

        // Read the file.
        File inFile = new File(inFilePath);
        String fileContent = FileUtils.readFileToString(inFile, "UTF-8");
        if(StringUtils.isEmpty(fileContent)) {
            log.error("Error in beautify. %s was empty.", inFilePath);
            return;
        }

        String beautified = beautify(fileContent);
        if (StringUtils.isEmpty(beautified)) {
            log.error("beautify(%s) is empty.", inFilePath);
            return;
        }

        File outFile = new File(outFilePath);
        FileUtils.writeStringToFile(outFile, beautified, "UTF-8");
        log.debug("Beautified %s and stored it in %s.", inFile, outFile);
    }

    public void beautifyToFile(String minJS, String outFilePath) throws IOException {
        String beautified = beautify(minJS);
        if (StringUtils.isEmpty(beautified)) {
            log.error("beautify(%s) is empty.", outFilePath);
            return;
        }

        File outFile = new File(outFilePath);
        FileUtils.writeStringToFile(outFile, beautified, "UTF-8");
        log.debug("Beautified data and stored it in %s.", outFile);
    }
}