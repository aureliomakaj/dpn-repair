package org.example.dpnrepair;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new File("src/main/resources/figure1-dpn.pnml"));
        doc.getDocumentElement().normalize();
        Node node = doc.getDocumentElement();
//        for(int i = 0; i < node.; i++){
//            System.out.println(nodeList.item(0).);
//        }
    }
}