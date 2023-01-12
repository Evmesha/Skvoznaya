package def;

import def.handler.Handler;
import def.handler.PatternAction;
import def.handler.RegExpAction;
import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Getter
public class ParameterHandler extends Handler {
    //состояния при получении (файл первое действие + второе действие) распаршивать надо в обратном порядке
//    private String inputFileFirstAction;
//    private String inputFileSecondAction;
//    private String inputFileExtension;
//    private boolean designPatternOn;
//    private boolean regExpOn;
//    private Path inputFile;
//    private Path outputFile;
    protected boolean designPatternOn;
    protected boolean regExpOn;

    public ParameterHandler(String[] args) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        parseParam(args);
    }

    private void parseParam(String[] args) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        if (args[0].equalsIgnoreCase("zip") || args[0].equalsIgnoreCase("crypt")) {
            //служебные методы
            outputFile = checkPath(args[1]);
            switch (args[0]) {
                case "zip":
                    ParameterHandler.createZip(this);
                    break;
                case "crypt":
                    encrypt(outputFile);
                    break;
            }
            System.exit(0);
        }
        //сначала определяем в каком виде входной файл
        String[] inArr = args[0].split("\\+");
        checkFileParameterState(inArr);
        inputFileFirstAction = inArr[0];
        inputFileSecondAction = inArr[1];

        //формат файла txt, xml, json
        checkFileExtension(args[1]);

        //с дизайн паттерном или без
        checkDesignPattern(args[2]);

        // с регуляркой или без
        checkRegExp(args[3]);

        // путь к исходному файлу
        inputFile = checkPath(args[4]);

        //путь выходного файла
        outputFile = checkPath(args[5]);
        //с дизайн паттернами
        Builder builder = Builder.getInstance();

        if (designPatternOn) {
            PatternAction action = builder.getPatternAction();
            action.setInputFile(inputFile);
            action.setInputFileExtension(inputFileExtension);
            action.setInputFileFirstAction(inputFileFirstAction);
            action.setInputFileSecondAction(inputFileSecondAction);
            action.setRegExpOn(regExpOn);
            action.setOutputFile(outputFile);
            action.act();
        } else if (regExpOn) {
            RegExpAction action = builder.getRegExpAction();
            action.setInputFile(inputFile);
            action.setInputFileExtension(inputFileExtension);
            action.setInputFileFirstAction(inputFileFirstAction);
            action.setInputFileSecondAction(inputFileSecondAction);
            action.setDesignPatternOn(designPatternOn);
            action.setOutputFile(outputFile);
        }
    }

    private void checkFileParameterState(String[] arr) {
        for (String s : arr) {
            switch (s) {
                case "ziped":
                    break;
                case "unziped":
                    break;
                case "crypted":
                    break;
                case "uncrypted":
                    break;
                default:
                    System.err.println("Wrong type of parameter " + s + ". Try again.");
                    System.exit(0);
            }
        }
    }

    private void checkFileExtension(String s) {
        if (("txt".equalsIgnoreCase(s) || "xml".equalsIgnoreCase(s) || "json".equalsIgnoreCase(s))) {
            inputFileExtension = s;
        } else {
            System.err.println("Wrong type of file " + s + ". Try again.");
            System.exit(0);
        }
    }

    private void checkDesignPattern(String arg) {
        switch (arg) {
            case "Pttrn":
                designPatternOn = true;
                break;
            case "NoPttrn":
                designPatternOn = false;
                break;
            default:
                System.err.println("There is a typo in pattern option: " + arg + ". Use only Pttrn/NoPttrn (case sensitive). Try again.");
                System.exit(0);
        }
    }

    private void checkRegExp(String arg) {
        switch (arg) {
            case "RegExp":
                regExpOn = true;
                break;
            case "NoRegExp":
                regExpOn = false;
                break;
            default:
                System.err.println("There is a typo in regexp option: " + arg + ". Use only RegExp/NoRegExp (case sensitive). Try again.");
                System.exit(0);
        }
    }

    private Path checkPath(String arg) {
        try {
            return Paths.get(arg);
        } catch (InvalidPathException e) {
            System.err.println("Wrong filepath: " + arg + ". Try again.");
            System.exit(0);
        }
        return null;
    }

    public void execute() throws IOException, JAXBException, TransformerException, ParserConfigurationException, SAXException {
        FileWriter fw = new FileWriter(outputFile.toFile());
        if (inputFileSecondAction.equalsIgnoreCase("ziped") || inputFileSecondAction.equalsIgnoreCase("crypted")
                || (inputFileFirstAction.equalsIgnoreCase("ziped") || inputFileFirstAction.equalsIgnoreCase("crypted"))) {
            //сначала вторую операцию запиливаем взад
            if (inputFileSecondAction.equalsIgnoreCase("ziped")) {
                openZip(this);
            }
            if (inputFileSecondAction.equalsIgnoreCase("crypted")) {
                ParameterHandler.encrypt(inputFile = Paths.get(inputFile.toFile().getAbsolutePath().substring(0, inputFile.toFile().getAbsolutePath().lastIndexOf("."))));
            }
            if (inputFileFirstAction.equalsIgnoreCase("ziped")) {
                openZip(this);
            }
            if (inputFileFirstAction.equalsIgnoreCase("crypted")) {
                ParameterHandler.encrypt(inputFile = Paths.get(inputFile.toFile().getAbsolutePath().substring(0, inputFile.toFile().getAbsolutePath().lastIndexOf("."))));
                inputFile = Paths.get(inputFile.toFile().getAbsolutePath() + ".encrypt");

            }
        }
        {//если исходник не архивирован и не зашифрован
            //проверить тип файла
            if (inputFileExtension.equalsIgnoreCase("txt")) {
                BufferedReader br = new BufferedReader(new FileReader(getInputFile().toFile()));
                StringBuilder sb = new StringBuilder();
                while (br.ready()) {
                    //читаем по строке и ищем операции
                    String in = br.readLine();
                    //без регулярок
                    String newString = ParameterHandler.changeString(in);
                    fw.write(newString);
                    fw.close();

                }
            } else if (inputFileExtension.equalsIgnoreCase("xml")) {
                String out = ParameterHandler.xmlReader(inputFile);
                xmlWriter( ParameterHandler.changeString(out), getOutputFile());
            } else if (inputFileExtension.equalsIgnoreCase("json")) {
                //json прочитать и распарсить
                String[] arr = jsonReader(inputFile);
                List<String> filtered = new ArrayList<>();
                for (String s : arr) {
                    filtered.add(ParameterHandler.changeString(s));
                }
                jsonWriter(filtered.toArray(new String[0]), getOutputFile());
            }
        }

        //делаем как было
        if (inputFileFirstAction.equalsIgnoreCase("ziped")) {
            ParameterHandler.createZip(this);
            outputFile = Paths.get(outputFile.toFile().getAbsolutePath() + ".zip");
        }
        if (inputFileFirstAction.equalsIgnoreCase("crypted")) {
            ParameterHandler.encrypt(outputFile);
            outputFile = Paths.get(outputFile.toFile().getAbsolutePath() + ".encrypt");
        }

        if (inputFileSecondAction.equalsIgnoreCase("ziped")) {
            ParameterHandler.createZip(this);
        }
        if (inputFileSecondAction.equalsIgnoreCase("crypted")) {
            ParameterHandler.encrypt(outputFile);
        }
        fw.close();
    }

    //шифрование и запись на диск
    public static byte[] encrypt(Path file) throws IOException {
        //сделать массив байт наоборот
        byte[] inArr = Files.readAllBytes(file);
        for (int i = 0; i < inArr.length / 2; i++) {
            byte tmp = inArr[i];
            inArr[i] = inArr[inArr.length - i - 1];
            inArr[inArr.length - i - 1] = tmp;
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toFile().getAbsoluteFile() + ".encrypt"));
        bos.write(inArr);
        bos.flush();
        bos.close();
        return inArr;
    }

    public static void openZip(Handler handler) throws IOException {
        String fileZip = handler.getInputFile().toString();
        File destDir = new File(handler.getInputFile().toFile().getAbsoluteFile().getParent());

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        String tempPath = handler.getInputFile().toFile().getAbsolutePath();
        handler.setInputFile(Paths.get(tempPath.substring(0, tempPath.lastIndexOf("."))));
    }

    public static void createZip(Handler handler) throws IOException {
        String sourceFile = handler.getOutputFile().toString();
        FileOutputStream fos = new FileOutputStream(handler.getOutputFile().toFile().getAbsoluteFile() + ".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        File fileToZip = new File(sourceFile);
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();
    }

    //считает подстроки с выражениями
    public static String changeString(String str) {
        List<Character> numbers = "0123456789".chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        List<Character> operands = "+-*/".chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        //проход по строке посимвольно
        StringBuilder stringBuilder = new StringBuilder(str);
        STRING:
        for (int i = 0; i < stringBuilder.length(); i++) {
            //поиск первого числового символа
            char ch1 = stringBuilder.charAt(i);
            if (numbers.contains(ch1)) {//если нашли число
                //дальше ищем число или операнд
                for (int j = i + 1; j < stringBuilder.length() - 1; j++) {
                    char ch2 = stringBuilder.charAt(j);
                    if (numbers.contains(ch2) || operands.contains(ch2)) {
                        if (operands.contains(ch2)) {//если операнд, то дальше должно быть число
                            //когда число заканчивается - подстрока готова к расчету
                            INNER:
                            for (int k = j + 1; k <= stringBuilder.length(); k++) {
                                if (k == stringBuilder.length()) {
                                    String v = calc(new String[]{stringBuilder.substring(i, k)}).get(0);
                                    stringBuilder.replace(i, k, v);
                                    break INNER;
                                }
                                char ch3 = stringBuilder.charAt(k);
                                if (!numbers.contains(ch3)) {
                                    String v = calc(new String[]{stringBuilder.substring(i, k)}).get(0);
                                    stringBuilder.replace(i, k, v);
                                    i += v.length();
                                    continue STRING;
                                } else {
                                }
                            }
                        }
                    } else {
                        continue STRING;
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    private static List<String> calc(String[] arr) {
        List<String> list = new ArrayList<>();
        char[] operand = "+-*/".toCharArray();
        int first, second;
        for (String s : arr) {
            for (int i = 0; i < s.length(); i++) {
                for (char o : operand) {
                    if (s.charAt(i) == o) {
                        first = Integer.parseInt(s.substring(0, i));
                        second = Integer.parseInt(s.substring(i + 1));
                        switch (o) {
                            case '+':
                                list.add(String.valueOf(first + second));
                                break;
                            case '-':
                                list.add(String.valueOf(first - second));
                                break;
                            case '*':
                                list.add(String.valueOf(first * second));
                                break;
                            case '/':
                                list.add(String.valueOf((double) first / second));
                                break;
                        }
                    }
                }
            }
        }
        return list;
    }

    public static void xmlWriter(String arr, Path outputFile) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element root = document.createElement("root");
        document.appendChild(root);

        root.appendChild(document.createTextNode(arr));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        StreamResult file = new StreamResult((outputFile.toFile()));
        transformer.transform(source, file);
    }

    //читаем элемент с именем "root" из xml
    public static String xmlReader(Path inputFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputFile.toFile());
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        String inputStr;
        inputStr = root.getTextContent();
        return inputStr;

    }

    public static String[] jsonReader(Path inputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputFile.toFile(), String[].class);

    }

    public static void jsonWriter(String[] arr, Path outputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new FileWriter(outputFile.toFile()), arr);
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
