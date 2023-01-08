
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParameterHandlerTest {

    @BeforeAll
    static void prep() throws IOException, ParserConfigurationException, TransformerException {
        //для текстовых файлов
        FileWriter fw = new FileWriter("c:/test/jtest.txt");
        fw.write("1+2 ewd2+3 4*22 asdasddas44/2 4/5&!%sad 4*3 ");
        fw.flush();
        fw.close();
        //для xml
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("root");
        document.appendChild(root);
        root.appendChild(document.createTextNode("1+2 ewd2+3 4*22 asdasddas44/2 4/5&!%sad 4*3 "));
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        StreamResult file = new StreamResult(("c:/test/jtest.xml"));
        transformer.transform(source, file);

    }

    @Test
    void changeString() throws IOException {
        //ARRANGE
        String start = "1+2 ewd2+3 4*22 asdasddas44/2 4/5&!%sad 4*3 ";
        String expect = "3 ewd5 88 asdasddas22.0 0.8&!%sad 12 ";
        //ACT
        String res = ParameterHandler.changeString(start);
        //CHECK
        Assertions.assertEquals(expect, res);
        Assertions.assertNotEquals(start, res);
    }

    @Test
    void encrypt() throws IOException {
        //ARRANGE
        byte[] inArr = Files.readAllBytes(Paths.get("c:/test/jtest.txt"));
        for (int i = 0; i < inArr.length / 2; i++) {
            byte tmp = inArr[i];
            inArr[i] = inArr[inArr.length - i - 1];
            inArr[inArr.length - i - 1] = tmp;
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File("c:/test/jtest.txt").getAbsoluteFile() + ".encrypt"));
        bos.write(inArr);
        bos.flush();
        bos.close();
        //ACT
        ParameterHandler.encrypt(Paths.get("c:/test/jtest.txt"));
        //CHECK
        Assertions.assertTrue(Files.exists(Paths.get("c:/test/jtest.txt.encrypt")));
    }

    @Test
    void xmlWriter() throws TransformerException, ParserConfigurationException, IOException, SAXException {
        //ARRANGE
        String start = "1+2 2+3 4*2 4/2 4/4 4*3asdjhkjasdh5+3";
        //ACT
        ParameterHandler.xmlWriter(start, Paths.get("c:/test/jtest.xml"));
        //CHECK
        String strings = ParameterHandler.xmlReader(Paths.get("c:/test/jtest.xml"));
        Assertions.assertTrue(strings.contains(start));
    }

    @Test
    void xmlReader() throws IOException, SAXException, ParserConfigurationException {
        //ARRANGE
        String expect = "1+2 ewd2+3 4*22 asdasddas44/2 4/5&!%sad 4*3 ";
        //ACT
        String res = ParameterHandler.xmlReader(Paths.get("c:/test/jtest.xml"));
        //CHECK
        Assertions.assertEquals(expect,res);
    }

    @Test
    void jsonReader() throws IOException {
        //ARRANGE
        String[] start = {"1+2", "2+3", "4*2", "4/2", "4/4", "4*3asdjhkjasdh5+3"};
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new FileWriter("c:/test/jtest.json"), start);
        //ACT
        String[] res = ParameterHandler.jsonReader(Paths.get("c:/test/jtest.json"));
        //CHECK
        Assertions.assertTrue(Arrays.equals(start, res));
    }

    @Test
    void jsonWriter() throws IOException {
        //ARRANGE
        String[] start = {"1+2", "2+3", "4*2", "4/2", "4/4", "4*3asdjhkjasdh5+3"};
        //ACT
        ParameterHandler.jsonWriter(start, Paths.get("c:/test/jtest.json"));
        //CHECK
        ObjectMapper mapper = new ObjectMapper();
        String[] read = mapper.readValue(new File("c:/test/jtest.json"), String[].class);
        Assertions.assertTrue(Arrays.equals(start, read));
    }

    @AfterAll
    static void del() {
        new File("c:/test/jtest.txt").delete();
        new File("c:/test/jtest.txt.encrypt").delete();
        new File("c:/test/jtest.xml").delete();
        new File("c:/test/jtest.json").delete();
    }
}