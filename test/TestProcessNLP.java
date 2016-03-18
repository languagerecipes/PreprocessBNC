/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import edu.stanford.nlp.parser.common.ParserGrammar;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import ie.pars.bnc.preprocess.ProcessNLP;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.TreeSet;

/**
 *
 * @author bq
 */
public class TestProcessNLP {

    private static MaxentTagger tagger;
    private static Morphology m;
    private static ParserGrammar parser;
    
    static String pathOutput;
    static String pathInput;

    public static void main(String[] sugar) throws FileNotFoundException, Exception {
        String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
        String parseModel = LexicalizedParser.DEFAULT_PARSER_LOC;

        pathInput = sugar[0];
        pathOutput = sugar[1];
        tagger = new MaxentTagger(taggerPath);
        m = new Morphology();
        parser = ParserGrammar.loadModel(parseModel);
        parser.loadTagger();

        File f = new File(sugar[0]);
        String id = f.getName().split(".xml")[0];
        System.out.println("dumping result in  " + pathOutput + File.separatorChar + id + ".vert");
        OutputStream out
                = new FileOutputStream(pathOutput + File.separatorChar + id + ".vert");
        Writer writer = new OutputStreamWriter(out, "UTF-8");

        InputStream inputStreamFile = new FileInputStream(f);

        ProcessNLP.readTokenizeParseBNCXMLFile(inputStreamFile, m, tagger, parser, writer, id);
        writer.close();
        // System.out.println(parseBNCXML);
    }

}
