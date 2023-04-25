package org.example.dpnrepair;

import org.example.dpnrepair.parser.DPNParser;
import org.example.dpnrepair.exceptions.DPNParserException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, DPNParserException {
        DPNParser parser = new DPNParser("src/main/resources/figure1-dpn.pnml");
        parser.parse();
        System.out.println("Finished");
    }
}