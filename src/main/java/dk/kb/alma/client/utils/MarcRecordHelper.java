package dk.kb.alma.client.utils;

import com.google.common.base.Charsets;
import dk.kb.alma.client.exceptions.MarcXmlException;
import dk.kb.alma.gen.bibs.Bib;
import dk.kb.util.xml.XML;
import org.apache.commons.io.IOUtils;
import org.marc4j.MarcXmlReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class MarcRecordHelper {
    
    public static final String DF245_TAG = "245"; // Title
    static final String DF500_TAG = "500"; // Note field
    public static final String DF035_TAG = "035"; // Network number
    
    
    /**
     * Helper class for editing marcXml on an Alma record
     */
    
    private static final Logger log = LoggerFactory.getLogger(MarcRecordHelper.class);
    
    private static final char TITLE_CODE = 'a';
    private static final String AUTHOR_TAG = "100";
    private static final char AUTHOR_CODE = 'a';
    
    /**
     * Create a title field when creating a new Bib record
     *
     * @param almaRecord The new Bib record that gets the title
     */
    public static void createRecordWithTitle(Bib almaRecord) {
        try {
            MarcFactory marcFactory = MarcFactory.newInstance();
            Record marcRecord = marcFactory.newRecord();
            // Add minimum contents for creating a new Bib record (i.e. the title)
            DataField dataField = marcFactory.newDataField(DF245_TAG, '1', '0');
            dataField.addSubfield(marcFactory.newSubfield(TITLE_CODE, "NewTitle"));
            marcRecord.addVariableField(dataField);
            MarcRecordHelper.saveMarcRecordOnAlmaRecord(almaRecord, marcRecord);
        } catch (MarcXmlException e) {
            log.error("Could not create marc record {} for Bib {}.",
                      almaRecord.getAnies().stream().findFirst().toString(), almaRecord.getMmsId());
//            throw new MarcXmlException("Failed to create new record: " + almaRecord.getMmsId());
        }
    }
    
    public static Record getMarcRecordFromAlmaRecord(Bib almaRecord) throws MarcXmlException {
        Node marcXmlNode;
        int anySize = almaRecord.getAnies().size();
        if (anySize == 4) {
            marcXmlNode = almaRecord.getAnies().get(3);
        } else if (anySize == 1) {
            marcXmlNode = almaRecord.getAnies().get(0);
        } else {
            throw new MarcXmlException("Wrong number of marcXml objects:  " + almaRecord.getAnies().size() +
                                       " was found on Alma record with id: " + almaRecord.getMmsId());
        }
        
        try (InputStream marcXmlStream = IOUtils.toInputStream(XML.domToString(marcXmlNode), StandardCharsets.UTF_8)) {
            MarcXmlReader marcXmlReader = new MarcXmlReader(marcXmlStream);
            Record marcRecord;
            if (marcXmlReader.hasNext()) {
                marcRecord = marcXmlReader.next();
            } else {
                throw new MarcXmlException(
                        "No marc record found in marcXml on Alma record with id: " + almaRecord.getMmsId());
            }
            if (marcXmlReader.hasNext()) {
                throw new MarcXmlException("Multiple marc records found in marcXml on Alma record with id: " +
                                           almaRecord.getMmsId());
            }
            return marcRecord;
        } catch (TransformerException | IOException e) {
            throw new MarcXmlException("Failed to read marcXml from Alma record with id: " + almaRecord.getMmsId(), e);
        }
    }
    
    public static void saveMarcRecordOnAlmaRecord(Bib almaRecord, Record marcRecord) throws MarcXmlException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MarcXmlWriter marcXmlWriter = new MarcXmlWriter(out);
            marcXmlWriter.write(marcRecord);
            marcXmlWriter.close();
            String marcXmlString = out.toString(String.valueOf(Charsets.UTF_8));
            Element marcElement = XML.fromXML(marcXmlString, false).getDocumentElement();
            
            almaRecord.getAnies().clear();
            almaRecord.getAnies().add(marcElement);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new MarcXmlException("Failed to save marc record on Alma record");
        }
    }
    
    /**
     * Sets the title on the marc record. Assumes that the field already exists
     *
     * @return true if the title is successfully set false if the record is missing the field
     */
    public static boolean setTitle(Record marcRecord, String title) {
        return setDataField(marcRecord, DF245_TAG, TITLE_CODE, title);
    }
    
    /**
     * Sets the author on the marc record. Assumes that the field already exists
     *
     * @return true if the author is successfully set. false if the record is missing the field
     */
    public static boolean setAuthor(Record marcRecord, String author) {
        return setDataField(marcRecord, AUTHOR_TAG, AUTHOR_CODE, author);
    }
    
    /**
     * Update contents for an existing data field on a Marc record
     *
     * @param marcRecord    The Marc record
     * @param dataFieldTag  tag, e.g. "100" (Author)
     * @param subFieldCode  code (E.g. 'a')
     * @param subfieldValue value (E.g. "Andersen, H.C.")
     * @return false if the field is not present otherwise true
     */
    
    public static boolean setDataField(Record marcRecord, String dataFieldTag, char subFieldCode,
                                       String subfieldValue) {
        DataField field = (DataField) marcRecord.getVariableField(dataFieldTag);
        if (field == null) {
            return false;
        }
        Subfield subfield = field.getSubfield(subFieldCode);
        if (subfield == null) {
            return false;
        }
        field.getSubfields().get(0).setData(subfieldValue);
        return true;
    }
    
    /**
     * Add a new @dataField with one subfield to a Marc record
     *
     * @param marcRecord    The record to add new datafield to
     * @param dataFieldTag  The tag to add (E.g. "100", "500")
     * @param dataFieldInd1 (E.g. '1')
     * @param dataFieldInd2 (E.g. '0' )
     * @param subfieldCode  (E.g. 'a')
     * @param subfieldValue The text value of the subfield
     */
    public static void addDataField(Record marcRecord, String dataFieldTag, char dataFieldInd1, char dataFieldInd2,
                                    char subfieldCode, String subfieldValue) {
        
        MarcFactory marcFactory = MarcFactory.newInstance();
        DataField dataField = marcFactory.newDataField(dataFieldTag, dataFieldInd1, dataFieldInd2);
        dataField.addSubfield(marcFactory.newSubfield(subfieldCode, subfieldValue));
        marcRecord.addVariableField(dataField);
    }
    
    /**
     * Add a new @dataField with more subfields to a Marc record
     *
     * @param marcRecord    The record to add new datafield to
     * @param dataFieldTag  The tag to add (E.g. "100", "500")
     * @param dataFieldInd1 (E.g. '1')
     * @param dataFieldInd2 (E.g. ' ' )
     * @param subFields     a list of the subfields to add
     */
    
    public static void addDataField(Record marcRecord, String dataFieldTag, char dataFieldInd1, char dataFieldInd2,
                                    List<Subfield> subFields) {
        
        MarcFactory marcFactory = MarcFactory.newInstance();
        DataField dataField = marcFactory.newDataField(dataFieldTag, dataFieldInd1, dataFieldInd2);
        for (Subfield subfield : subFields) {
            char code = subfield.getCode();
            String data = subfield.getData();
            dataField.addSubfield(marcFactory.newSubfield(code, data));
        }
        marcRecord.addVariableField(dataField);
    }
    
    /**
     * Add a subfield to an existing field
     *
     * @param marcRecord    The marc record to add the subfield to
     * @param dataFieldTag  The tag to add the subfield to, e.g. "100"
     * @param subfieldCode  The code of the subfield, e.g. 'b'
     * @param subfieldValue The data of the subfield e.g. "Some new data"
     */
    public static void addSubfield(Record marcRecord, String dataFieldTag, char subfieldCode, String subfieldValue) {
        MarcFactory marcFactory = MarcFactory.newInstance();
        DataField dataField = null;
        try {
            dataField = (DataField) marcRecord.getVariableField(dataFieldTag);
        } catch (NullPointerException e) {
            log.info("DataField {} was not found", dataFieldTag);
            e.printStackTrace();
        }
        if (dataField != null) {
            dataField.addSubfield(marcFactory.newSubfield(subfieldCode, subfieldValue));
//            marcRecord.addVariableField(dataField);
        }
    }
    
    public static String getControlField(Record marcRecord, String tag) {
        try {
            ControlField cf = (ControlField) marcRecord.getVariableField(tag);
            return cf.getData();
        } catch (NullPointerException e) {
            log.info("DataField {} was not found", tag);
            return null;
        }
    }
    
    public static void setControlField008(Record almaMarcRecord, Record marcRecord, String digiYear) {
        String tag = "008";
        try {
            String controlField008ana = MarcRecordHelper.getControlField(almaMarcRecord, tag);
            String controlField008digi = MarcRecordHelper.getControlField(marcRecord, tag);
            assert controlField008digi != null;
            assert controlField008ana != null;
            
            String language35to37 = "|||";
            try {
                language35to37 = controlField008ana.substring(35, 38);
            } catch (Exception e) {
                log.info("Language not found in Controlfield 008");
                e.printStackTrace();
            }
            String country15to16 = controlField008ana.substring(15, 17);
            String originYear11to14 = controlField008ana.substring(7, 11);
            
            String cf008_00to05 = controlField008digi.substring(0, 6);
            String cf008_06 = "r";
            String cf008_17to22 = controlField008digi.substring(17, 23).replace('#', '|');
            String cf008_23 = "s";
            String cf008_24to34 = controlField008digi.substring(24, 35).replace('#', '|');
            String cf008_38to39 = controlField008digi.substring(38, 40).replace('#', '|');
            
            String newControlField008digi = cf008_00to05
                                            + cf008_06
                                            + digiYear // 7to10
                                            + originYear11to14
                                            + country15to16
                                            + cf008_17to22
                                            + cf008_23
                                            + cf008_24to34
                                            + language35to37
                                            + cf008_38to39;
            
            MarcFactory marcFactory = MarcFactory.newInstance();
            marcRecord.removeVariableField(marcRecord.getVariableField(tag));
            marcRecord.addVariableField(marcFactory.newControlField(tag, newControlField008digi));
        } catch (NullPointerException e) {
            log.warn("Setting ControlField {} failed", tag);
        }
    }
  
    
    public static String getSubfieldValue(Record marcRecord, String tag, Character subfieldTag) {
        return getDataFields(marcRecord, tag).stream().findFirst().map(dataField -> dataField.getSubfield(subfieldTag)).filter(
                Objects::nonNull).map(subfield -> subfield.getData()).orElse(null);
    }
    
    
    public static List<String> getSubfieldValues(Record marcRecord, String tag, Character subfieldTag) {
        return getDataFields(marcRecord, tag).stream().map(dataField -> dataField.getSubfield(subfieldTag)).filter(
                Objects::nonNull).map(subfield -> subfield.getData()).collect(Collectors.toList());
    }
    
    
    public static boolean isSubfieldPresent(Record marcRecord, String tag, Character subfield) {
        return getDataFields(marcRecord, tag).stream()
                                            .flatMap(dataField -> dataField.getSubfields().stream())
                                            .anyMatch(subfield1 -> subfield1.getCode() == subfield);
    }
    
    public static DataField getDataField(Record marcRecord, String tag) {
        return marcRecord.getDataFields().stream().filter(datafield -> fieldMatches(datafield, tag)).findFirst().orElse(null);
    }
    
    public static List<DataField> getDataFields(Record marcRecord, String tag) {
        return marcRecord.getDataFields().stream().filter(datafield -> fieldMatches(datafield, tag)).collect(
                Collectors.toList());
    }
    
    
    private static boolean fieldMatches(final DataField field, final String tag) {
        if (field.getTag().equals(tag)) {
            return true;
        }
        if (tag.startsWith("LNK") && field.getTag().equals("880")) {
            final DataField df = field;
            final Subfield link = df.getSubfield('6');
            if (link != null && link.getData().equals(tag.substring(3))) {
                return true;
            }
        }
        return false;
    }
    
   
    /**
     * Get all fields in the list of tags
     *
     * @param almaMarcRecord The Alma marcrecord to get fields from
     * @param marcRecord     The marc record to add the fields to
     * @param tags           The list of tags to copy
     */
    public static void getVariableField(Record almaMarcRecord, Record marcRecord, String[] tags) {
        for (String tag : tags) {
            try {
                marcRecord.addVariableField(almaMarcRecord.getVariableField(tag));
            } catch (NullPointerException e) {
                log.info("DataField {} was not found", tag);
            }
        }
    }
    
    private final static Pattern ANAREF_PATTERN = Pattern.compile("^(x[0-9]{9})$");
    
    public static void removeAnaReference(Record digiMarcRecord) {
        
        try {
            List<VariableField> variableFields = digiMarcRecord.find("035", "x");
            DataField df = (DataField) variableFields.get(0);
            String a = df.getSubfield('a').getData();
            if (ANAREF_PATTERN.matcher(a).matches()) {
                digiMarcRecord.removeVariableField(df);
            }
        } catch (NullPointerException e) {
            log.info("Analog reference was not found or not removed");
        }
        
        
    }
    
    public static String getNetworkNumber(Record marcRecord, String searchValue) {
        String tag = DF035_TAG;
        try {
            List<VariableField> variableFields = marcRecord.find(searchValue);
            DataField df = (DataField) variableFields.get(0);
            return df.getSubfield('a').getData();
        } catch (NullPointerException e) {
            log.info("DataField {} was not found", tag);
            return null;
        }
    }
    
    //    /**
//     * Extract the periodical type from an alma Bib record
//     * @param almaRecord
//     * @return the periodical type. The default is JOURNAL if the type cannot otherwise be determined
//     */
//
//        public static ElbaFacade.PeriodicalType getPeriodicalType(Bib almaRecord) {
//            ElbaFacade.PeriodicalType periodicalType = ElbaFacade.PeriodicalType.JOURNAL; //default
//            try {
//                Record marcRecord = MarcRecordHelper.getMarcRecordFromAlmaRecord(almaRecord);
//                final char typeChar = marcRecord.getVariableField("008").toString().charAt(25);
//                if (typeChar == 'n') {
//                    periodicalType = ElbaFacade.PeriodicalType.NEWS;
//                } else if (typeChar == 'p')  {
//                    periodicalType = ElbaFacade.PeriodicalType.JOURNAL;
//                } else {
//                    log.warn("Didn't find periodical type in {} for {}, assuming Journal.", marcRecord
//                    .getVariableField("008"), almaRecord.getMmsId());
//                    periodicalType = ElbaFacade.PeriodicalType.JOURNAL;
//                }
//            } catch (MarcXmlException e) {
//                log.warn("Could not parse marc record {} in Bib {}.", almaRecord.getAny().stream().findFirst()
//                .toString(), almaRecord.getMmsId());
//            }
//            return periodicalType;
//        }
    
    
    
    /**
     * Get all occurrences with the specified tag
     *
     * @param fromMarcRecord The (Alma) Marcrecord to retrieve data from
     * @param toMarcRecord The (Marc) record to copy data to
     * @param tag        The tag to copy from
     */
    public static void copyVariableFields(Record fromMarcRecord, Record toMarcRecord, String tag) {
        List<VariableField> variableFields = fromMarcRecord.getVariableFields(tag);
        for (VariableField vf : variableFields) {
            try {
                toMarcRecord.addVariableField(vf);
            } catch (NullPointerException e) {
                log.info("DataField {} was not found", tag);
            }
        }
    }
    
    public static void replaceVariableField(Record fromMarcRecord, Record toMarcRecord, String tag){
        try {
            toMarcRecord.removeVariableField(toMarcRecord.getVariableField(tag));
            toMarcRecord.addVariableField(fromMarcRecord.getVariableField(tag));
        } catch (NullPointerException e) {
            log.warn("Replacing Variable Field {} failed", tag);
        }
    }
    
}

