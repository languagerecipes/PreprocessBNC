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

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.parser.common.ParserGrammar;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Behrang QasemiZadeh <me at atmykitchen.info>
 */
public class ProcessNLP {

    /**
     *
     * @param inputStreamFile
     * @param morphology
     * @param posTagger
     * @param parser
     * @return
     * @throws Exception
     */
    public static StringBuilder parseBNCXML(InputStream inputStreamFile, Morphology morphology, MaxentTagger posTagger, ParserGrammar parser) throws Exception {
        StringBuilder results = new StringBuilder();
        int counterSent = 0;
        List<List<List<WordLemmaTag>>> parseBNCXMLTokenized = parseBNCXMLTokenized(inputStreamFile);
        for (List<List<WordLemmaTag>> xparseBNCXMLL : parseBNCXMLTokenized) {
            results.append("<p>\n");
            for (List<WordLemmaTag> para : xparseBNCXMLL) {
                if (counterSent++ % 20 == 0) {
                    System.out.print(".");
                }
                results.append("<s>\n");
                List<TaggedWord> tagSentence = posTagger.tagSentence(para, true);

                Tree parseTree = parser.parse(tagSentence);

                GrammaticalStructure gs = parser.getTLPParams().getGrammaticalStructure(parseTree,
                        parser.treebankLanguagePack().punctuationWordRejectFilter(),
                        parser.getTLPParams().typedDependencyHeadFinder());

                Collection<TypedDependency> deps = gs.typedDependenciesCollapsedTree();
                SemanticGraph depTree = new SemanticGraph(deps);

                for (int i = 0; i < tagSentence.size(); ++i) {

                    int head = -1;
                    String deprel = null;
//                    if (depTree != null) {
                    Set<Integer> rootSet = depTree.getRoots().stream().map(IndexedWord::index).collect(Collectors.toSet());
                    IndexedWord node = depTree.getNodeByIndexSafe(i + 1);
                    if (node != null) {
                        List<SemanticGraphEdge> edgeList = depTree.getIncomingEdgesSorted(node);
                        if (!edgeList.isEmpty()) {
                            assert edgeList.size() == 1;
                            head = edgeList.get(0).getGovernor().index();
                            deprel = edgeList.get(0).getRelation().toString();
                        } else if (rootSet.contains(i + 1)) {
                            head = 0;
                            deprel = "ROOT";
                        }
                    }
                    //     }

                    // Write the token
                    TaggedWord lexHead = null;
                    if (head > 0) {
                        lexHead = tagSentence.get(head - 1);
                    }
                    results.append(line(i + 1, tagSentence.get(i), morphology, head, deprel, lexHead)).append("\n");
                }
                results.append("</s>\n");
            }
            results.append("</p>\n");
        }
        System.out.println("");
        inputStreamFile.close();

        return results;
    }

  
    public static void handleDependencies(Tree tree, ParserGrammar parser, String arg, OutputStream outStream, String commandArgs)
            throws IOException {
        GrammaticalStructure gs = parser.getTLPParams().getGrammaticalStructure(tree,
                parser.treebankLanguagePack().punctuationWordRejectFilter(),
                parser.getTLPParams().typedDependencyHeadFinder());

        Collection<TypedDependency> deps = gs.typedDependenciesCollapsedTree();
        // SemanticGraph sg = new SemanticGraph(deps);

        OutputStreamWriter osw = new OutputStreamWriter(outStream, "utf-8");
        for (TypedDependency dep : deps) {
            String t = dep.dep().word() + "\t" + dep.dep().lemma() + "\t" + dep.dep().tag() + "\t";
            System.out.println(t);

            osw.write(dep.toString());
            osw.write("\n");
        }
        osw.flush();
    }

    public static List<List<List<WordLemmaTag>>> parseBNCXMLTokenized(InputStream is) throws Exception {
        DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        Element root = doc.getDocumentElement();
        List< List<List<WordLemmaTag>>> file = new ArrayList();
        List<List<WordLemmaTag>> para = null;
        NodeList sentences = root.getElementsByTagName("s");
        Node currentNode = null;
        for (int j = 0; j < sentences.getLength(); j++) {
            if (currentNode == null) { // if this is the first sentence
                currentNode = sentences.item(j).getParentNode();
                para = new ArrayList();
            } else {
                if (currentNode != sentences.item(j).getParentNode()) {
                    file.add(para);
                    para = new ArrayList();
                }
                currentNode = sentences.item(j).getParentNode();
            }
            List<WordLemmaTag> tokens = new ArrayList<>();
            for (int i = 0; i < sentences.item(j).getChildNodes().getLength(); i++) {
                if ("pause".equalsIgnoreCase(sentences.item(j).getChildNodes().item(i).getNodeName())) {
                    if (!tokens.isEmpty()) {
                        para.add(tokens);
                    }
                    tokens = new ArrayList<>();
                }
                if (sentences.item(j).getChildNodes().item(i).getTextContent().trim().length() != 0) {
                    tokens.add(new WordLemmaTag(sentences.item(j).getChildNodes().item(i).getTextContent().trim()));
                }
            }
            if (!tokens.isEmpty()) {
                para.add(tokens);
            }

        }
        if (para != null) {
            file.add(para);
        }

        return file;
    }

    
    public static void readTokenizeParseBNCXMLFile(InputStream is, 
            Morphology morphology, MaxentTagger posTagger, ParserGrammar parser,  Writer writer, String id ) throws Exception {
        DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);
        Element root = doc.getDocumentElement();
        //List< List<List<WordLemmaTag>>> file = new ArrayList();
        //List<List<WordLemmaTag>> para = null;
        NodeList sentences = root.getElementsByTagName("s");
        Node currentNode = null;
        int paraCount=1;
        int sentID=1;
       
        writer.write("<text id='"+ id+"'>\n");
        for (int j = 0; j < sentences.getLength(); j++) {
            if (currentNode == null) { // if this is the first sentence
                currentNode = sentences.item(j).getParentNode();
                writer.write("<p id=' "+ id+"-"+(paraCount++) +"'>\n");
                
            } else {
                if (currentNode != sentences.item(j).getParentNode()) {
                    writer.write("</p>\n");
                    writer.write("<p id=' "+ id +"-"+(paraCount++) +"'>\n");
                }
                currentNode = sentences.item(j).getParentNode();
            }
            
            String textContent = sentences.item(j).getTextContent();
            if(textContent.trim().length()!=0){
                String sid = id+"-"+ (paraCount-1 )+"-"+sentID++;
                StringBuilder parseASentence = parseTheSentence(textContent.trim(), morphology, posTagger, parser, sid);
                writer.write(parseASentence.toString());
            }
        }
        if (currentNode != null) {
             writer.write("</p>\n");
        }
         writer.write("</text>\n"); 
    }
    /**
     * The return is the line consists of the word lemma tag depRelatoin
     * distanceToGoverner GovWord GovLemma GovTag
     *
     * @param index
     * @param token
     * @param m
     * @param head
     * @param deprel
     * @param headWord
     * @return
     */
    private static String line(int index,
            TaggedWord token,
            Morphology m,
            int head, String deprel, TaggedWord headWord) {
        ArrayList<String> fields = new ArrayList<>(16);

        // fields.add(Integer.toString(index)); // 1
        fields.add(orNull(token.word()));    // 2
        fields.add(orNull(m.lemma(token.word(), token.tag(), true)));   // 3
        fields.add(orNull(token.tag()));     // 4
        // fields.add(orNull(token.ner()));     // 5
        fields.add(deprel);

        if (head == 0) {
            fields.add(Integer.toString(head));  // 6
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);

        } else if (head > 0) {
            fields.add(Integer.toString(head - index));  // 6
            fields.add(headWord.word());
            fields.add(m.lemma(headWord.word(), headWord.tag(), true));
            fields.add(headWord.tag());

        } else {
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);
            fields.add(NULL_PLACEHOLDER);
        }

        return StringUtils.join(fields, "\t");
    }

    private static final String NULL_PLACEHOLDER = "_";

    private static String orNull(String in) {
        if (in == null) {
            return NULL_PLACEHOLDER;
        } else {
            return in;
        }
    }

//    private static void parseInspectSentence(List<WordLemmaTag> sentence ,Morphology morphology, MaxentTagger posTagger, ParserGrammar parser){
//        if(sentence.size()>50){
//            boolean breakS=false;
//            Pattern p = Pattern.compile("\\p{Punct}");
//
//            for (int i = sentence.size()/2; i < sentence.size()-1; i++) {
//               Matcher m = p.matcher(sentence.get(i).word());
//               if(m.find()){
//                   StringBuilder parseTheSentence = parseTheSentence(sentence.subList(0, i), morphology, posTagger, parser);
//                   StringBuilder parseTheSentence1 = parseTheSentence(sentence.subList(i+1, sentence.size()), morphology, posTagger, parser);
//               }else{
//                  // return parseTheSentence(sentence, morphology, posTagger, parser);
//               }
//            }
//            
//        }else{
//           // return parseTheSentence(sentence, morphology, posTagger, parser);
//        }
//        
//    }
    
    private static StringBuilder parseTheSentence(String sentence ,Morphology morphology, MaxentTagger posTagger, ParserGrammar parser, String sid){
        TokenizerFactory<Word> newTokenizerFactory = PTBTokenizerFactory.newTokenizerFactory();
//        TokenizerFactory<WordLemmaTag> tokenizerFactory;
//        TokenizerFactory<CoreLabel> factory = PTBTokenizer.factory(new CoreLabelTokenFactory() , "");
//        TokenizerFactory<Word> factory1 = PTBTokenizer.factory();
        
                
        StringBuilder results = new StringBuilder();
        results.append("<s id='"+sid+"'>\n");
      
        StringReader sr = new StringReader(sentence);
        Tokenizer<Word> tokenizer = newTokenizerFactory.getTokenizer(sr);
        List<Word> tokenize = tokenizer.tokenize();
        
        List<TaggedWord> tagSentence = posTagger.tagSentence(tokenize);

        Tree parseTree = parser.parse(tagSentence);

        GrammaticalStructure gs = parser.getTLPParams().getGrammaticalStructure(parseTree,
                parser.treebankLanguagePack().punctuationWordRejectFilter(),
                parser.getTLPParams().typedDependencyHeadFinder());

        Collection<TypedDependency> deps = gs.typedDependenciesCollapsedTree();
        SemanticGraph depTree = new SemanticGraph(deps);

        for (int i = 0; i < tagSentence.size(); ++i) {

            int head = -1;
            String deprel = null;
//                    if (depTree != null) {
            Set<Integer> rootSet = depTree.getRoots().stream().map(IndexedWord::index).collect(Collectors.toSet());
            IndexedWord node = depTree.getNodeByIndexSafe(i + 1);
            if (node != null) {
                List<SemanticGraphEdge> edgeList = depTree.getIncomingEdgesSorted(node);
                if (!edgeList.isEmpty()) {
                    assert edgeList.size() == 1;
                    head = edgeList.get(0).getGovernor().index();
                    deprel = edgeList.get(0).getRelation().toString();
                } else if (rootSet.contains(i + 1)) {
                    head = 0;
                    deprel = "ROOT";
                }
            }
            //     }

            // Write the token
            TaggedWord lexHead = null;
            if (head > 0) {
                lexHead = tagSentence.get(head - 1);
            }
            results.append(line(i + 1, tagSentence.get(i), morphology, head, deprel, lexHead)).append("\n");
        }
        results.append("</s>\n");
        return results;
    }
}
