/* 
 * Copyright (C) 2016 Behrang QasemiZadeh <zadeh at phil.hhu.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ie.pars.bnc.preprocess;

import edu.stanford.nlp.parser.common.ParserGrammar;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.TreeSet;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 *
 * @author bq
 */
public class MainBNCProcess {

    private static MaxentTagger tagger;
    private static Morphology m;
    private static ParserGrammar parser;
    private static TreeSet<String> filesProcesed;
    static String letter;
    static String pathOutput;
    static String pathInput;

    /**
     * Main method use for processing BNC text. Claws PoSs are replaced by
     * Stanford CoreNLP results for consistency with the rest of data! Currently
     * the structure of BNC is mainly discarded, only paragraphs and sentences
     *
     * @param sugare
     * @throws IOException
     * @throws ArchiveException
     * @throws Exception
     */
    public static void main(String[] sugare) throws IOException, ArchiveException, Exception {
        pathInput = sugare[0];
        pathOutput = sugare[1];
        letter = sugare[2];
        filesProcesed = new TreeSet();

        File folder = new File(pathOutput);
        if (folder.exists()) {
            for (File f : folder.listFiles()) {
                if (f.isFile()) {
                    String pfile = f.getName().split("\\.")[0];
                    filesProcesed.add(pfile);
                }
            }
        } else {
            folder.mkdirs();
        }
        getZippedFile();
    }

    private static void getZippedFile() throws IOException, ArchiveException, Exception {
        String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
        String parseModel = LexicalizedParser.DEFAULT_PARSER_LOC;

        InputStream is = new FileInputStream(pathInput);
        TarArchiveInputStream tarStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        TarArchiveEntry entry = null;
        int countfiles = 0;
        while ((entry = (TarArchiveEntry) tarStream.getNextEntry()) != null) {
            //     for(File lf: listFiles){ 
            if (!entry.isDirectory()) {

                byte[] content = new byte[(int) entry.getSize()];
                int offset = 0;
                tarStream.read(content, offset, content.length - offset);
                String id = entry.getName().split("/")[entry.getName().split("/").length - 1].split(".xml")[0];

                if (!filesProcesed.contains(id) && id.startsWith(letter.toUpperCase())) {
                    if (countfiles++ % 10 == 0) {
                        tagger = new MaxentTagger(taggerPath);
                        m = new Morphology();
                        parser = ParserGrammar.loadModel(parseModel);
                        parser.loadTagger();
                    }
                    System.out.print("Entry " + entry.getName());

                    InputStream bis = new ByteArrayInputStream(content);
                    StringBuilder parseBNCXML = ProcessNLP.parseBNCXML(bis, m, tagger, parser);
                    bis.close();
                    OutputStream out = new FileOutputStream(pathOutput + File.separatorChar + id + ".vert");
                    Writer writer = new OutputStreamWriter(out, "UTF-8");

                    writer.write("<text id=\"" + id + "\">\n");
                    writer.write(parseBNCXML.toString());
                    writer.write("</text>\n");
                    writer.close();
                    out.close();
                } else {
                    System.out.println(">> Bypass Entry " + entry.getName());
                }
                //break;
            }

        }
        is.close();
        System.out.println("There are " + countfiles);
        //    tarStream.close();

    }

}
