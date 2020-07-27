package dk.kb.alma.client.sru;

import java.util.Arrays;

public abstract class Query {
    
    public abstract String build();
    
    public static Query greaterThan(Field field, String value) {
        return new Restriction(field, ">", value);
    }
    
    public static Query greaterThanEquals(Field field, String value) {
        return new Restriction(field, ">=", value);
    }
    
    public static Query lessThan(Field field, String value) {
        return new Restriction(field, "<", value);
    }
    
    public static Query lessThanEquals(Field field, String value) {
        return new Restriction(field, "<=", value);
    }
    
    public static Query containsWords(Field field, String value) {
        return new Restriction(field, "all", value);
    }
    
    public static Query containsPhrase(Field field, String value) {
        return new Restriction(field, "=", value);
    }
    
    public static Query equals(Field field, String value) {
        return new Restriction(field, "==", value);
    }
    
    public static Query notEquals(Field field, String value) {
        return new Restriction(field, "<>", value);
    }
    
    public static Query empty(Field field) {
        return new Restriction(field, "==", "");
    }
    
    public static Query sortAscending(SortableField field, Query query) {
        return new SortBy(field, false, query);
    }
    
    public static Query sortDescending(SortableField field, Query query) {
        return new SortBy(field, true, query);
    }
    
    public static Query and(Query... queries) {
        return new And(Arrays.asList(queries));
    }
    
    public static Query or(Query... queries) {
        return new Or(Arrays.asList(queries));
    }
    
    public static final Field accompanying_material=new Field("accompanying_material", "Accompanying Material");
    
    
    public static final Field additional_physical_form_available_note=new Field("additional_physical_form_available_note", "Additional Physical Form Available Note");
    
    
    public static final Field all_for_ui=new Field("all_for_ui", "Keywords");
    
    
    public static final Field alternate_complete_edition=new Field("alternate_complete_edition", "Edition");
    
    
    public static final Field authority_id=new Field("authority_id", "Authority Id");
    
    
    public static final Field authority_vocabulary=new Field("authority_vocabulary", "Authority Vocabulary");
    
    
    public static final Field bib_level=new Field("bib_level", "Bibliographic Level");
    
    
    public static final Field carrier_type_code=new Field("carrier_type_code", "Carrier Type Code (Title)");
    
    
    public static final Field carrier_type_term=new Field("carrier_type_term", "Carrier Type Term (Title)");
    
    
    public static final Field cartographic_mathematical_data=new Field("cartographic_mathematical_data", "Cartographic Mathematical Data");
    
    
    public static final Field category_of_material=new Field("category_of_material", "Physical Description");
    
    
    public static final Field classification_part=new Field("classification_part", "Classification Part");
    
    
    public static final Field coded_cartographic_mathematical_data=new Field("coded_cartographic_mathematical_data", "Coded Cartographic Mathematical Data");
    
    
    public static final Field content_related_data=new Field("content_related_data", "Content Related Data");
    
    
    public static final Field content_type_code=new Field("content_type_code", "Content Type Code");
    
    
    public static final Field content_type_term=new Field("content_type_term", "Content Type Term");
    
    
    public static final Field copyrights_note=new Field("copyrights_note", "Copyright Note");
    
    
    public static final Field country_of_publication=new Field("country_of_publication", "Country Of Publication");
    
    
    public static final Field creating_job_id=new Field("creating_job_id", "c.search.index_names.creating_job_id");
    
    
    public static final SortableField creator=new SortableField("creator", "Creator");
    
    
    public static final Field date_of_publication=new Field("date_of_publication", "Additional Publication Year");
    
    
    public static final Field dc_contributor=new Field("dc_contributor", "dc:contributor");
    
    
    public static final Field dc_coverage=new Field("dc_coverage", "dc:coverage");
    
    
    public static final Field dc_creator=new Field("dc_creator", "dc:creator");
    
    
    public static final Field dc_date=new Field("dc_date", "dc:date");
    
    
    public static final Field dc_description=new Field("dc_description", "dc:description");
    
    
    public static final Field dc_format=new Field("dc_format", "dc:format");
    
    
    public static final Field dc_language=new Field("dc_language", "dc:language");
    
    
    public static final Field dc_publisher=new Field("dc_publisher", "dc:publisher");
    
    
    public static final Field dc_relation=new Field("dc_relation", "dc:relation");
    
    
    public static final Field dc_rights=new Field("dc_rights", "dc:rights");
    
    
    public static final Field dc_source=new Field("dc_source", "dc:source");
    
    
    public static final Field dc_subject=new Field("dc_subject", "dc:subject");
    
    
    public static final Field dc_title=new Field("dc_title", "dc:title");
    
    
    public static final Field dc_type=new Field("dc_type", "dc:type");
    
    
    public static final Field description=new Field("description", "Description");
    
    
    public static final Field dewey_decimal_class_number=new Field("dewey_decimal_class_number", "Dewey Decimal Class Number");
    
    
    public static final Field digital_object_identifier=new Field("digital_object_identifier", "DOI - Digital Object Identifier");
    
    
    public static final Field elocation=new Field("elocation", "Electronic Location And Note");
    
    
    public static final Field event_date=new Field("event_date", "Event Date");
    
    
    public static final Field general_note=new Field("general_note", "Public Note (Title)");
    
    
    public static final Field genre_form=new Field("genre_form", "Genre Form");
    
    
    public static final Field geographic_area_code=new Field("geographic_area_code", "Geographic Area Code");
    
    
    public static final Field government_document_number=new Field("government_document_number", "Government Document Number");
    
    
    public static final Field granular_resource_type=new Field("granular_resource_type", "Granular Resource Type");
    
    
    public static final Field has_inventory=new Field("has_inventory", "Has Inventory");
    
    
    public static final Field identifier=new Field("identifier", "dc:identifier");
    
    
    public static final Field is_linked=new Field("is_linked", "Is linked");
    
    
    public static final Field isbn=new Field("isbn", "ISBN");
    
    
    public static final Field ismn=new Field("ismn", "Other Standard ID");
    
    
    public static final Field isni_identifier=new Field("isni_identifier", "ISNI - International Standard Name Identifier");
    
    
    public static final Field issn=new Field("issn", "ISSN");
    
    
    public static final Field issn_link=new Field("issn_link", "ISSN link");
    
    
    public static final Field language=new Field("language", "Language (Title)");
    
    
    public static final Field language_of_cataloging=new Field("language_of_cataloging", "Language Of Cataloging");
    
    
    public static final Field lc_class_number=new Field("lc_class_number", "LC Call Number");
    
    
    public static final Field lccn=new Field("lccn", "LC Control No.");
    
    
    public static final Field lcsh=new Field("lcsh", "Subjects (LC)");
    
    
    public static final Field link_inst=new Field("link_inst", "Linked Institution");
    
    
    public static final Field local_call_numbers=new Field("local_call_numbers", "Local call numbers");
    
    
    public static final Field local_control_field_009=new Field("local_control_field_009", "Local Control Field 009");
    
    
    public static final Field local_field_900=new Field("local_field_900", "Personal name reference");
    
    
    public static final Field local_field_901=new Field("local_field_901", "DG901-001a");
    
    
    public static final Field local_field_902=new Field("local_field_902", "DG902-440");
    
    
    public static final Field local_field_903=new Field("local_field_903", "DG903-500");
    
    
    public static final Field local_field_904=new Field("local_field_904", "DG904-501");
    
    
    public static final Field local_field_906=new Field("local_field_906", "Noc actual FAUST no.");
    
    
    public static final Field local_field_908=new Field("local_field_908", "danMARC2 codes");
    
    
    public static final Field local_field_909=new Field("local_field_909", "DG909-520");
    
    
    public static final Field local_field_910=new Field("local_field_910", "Corporate name reference");
    
    
    public static final Field local_field_912=new Field("local_field_912", "DK5");
    
    
    public static final Field local_field_913=new Field("local_field_913", "Contributor note (recorded music)");
    
    
    public static final Field local_field_914=new Field("local_field_914", "DG914-521");
    
    
    public static final Field local_field_915=new Field("local_field_915", "DG915-526");
    
    
    public static final Field local_field_916=new Field("local_field_916", "Controlled subject heading (DBC)");
    
    
    public static final Field local_field_917=new Field("local_field_917", "DG917-531");
    
    
    public static final Field local_field_918=new Field("local_field_918", "DG918-880");
    
    
    public static final Field local_field_919=new Field("local_field_919", "DG919-532");
    
    
    public static final Field local_field_920=new Field("local_field_920", "DG920-534");
    
    
    public static final Field local_field_921=new Field("local_field_921", "DG921-540");
    
    
    public static final Field local_field_922=new Field("local_field_922", "Type of binding");
    
    
    public static final Field local_field_923=new Field("local_field_923", "DG923-555");
    
    
    public static final Field local_field_924=new Field("local_field_924", "DG924-557");
    
    
    public static final Field local_field_925=new Field("local_field_925", "Data base category");
    
    
    public static final Field local_field_926=new Field("local_field_926", "DG926-745i");
    
    
    public static final Field local_field_927=new Field("local_field_927", "DG927-558");
    
    
    public static final Field local_field_928=new Field("local_field_928", "DG928-512e");
    
    
    public static final Field local_field_929=new Field("local_field_929", "DG929-860");
    
    
    public static final Field local_field_930=new Field("local_field_930", "DG930-861");
    
    
    public static final Field local_field_931=new Field("local_field_931", "DG931-863");
    
    
    public static final Field local_field_932=new Field("local_field_932", "Code for National bibliography (932)");
    
    
    public static final Field local_field_933=new Field("local_field_933", "DG933-865");
    
    
    public static final Field local_field_934=new Field("local_field_934", "DG934-866");
    
    
    public static final Field local_field_935=new Field("local_field_935", "DG935-867");
    
    
    public static final Field local_field_936=new Field("local_field_936", "DG936-868");
    
    
    public static final Field local_field_937=new Field("local_field_937", "DG937-870");
    
    
    public static final Field local_field_938=new Field("local_field_938", "Alternative call no.");
    
    
    public static final Field local_field_939=new Field("local_field_939", "Uniform title (music &amp; movies)");
    
    
    public static final Field local_field_941=new Field("local_field_941", "Syncronized language");
    
    
    public static final Field local_field_942=new Field("local_field_942", "Lix");
    
    
    public static final Field local_field_944=new Field("local_field_944", "FUI");
    
    
    public static final Field local_field_945=new Field("local_field_945", "Title reference");
    
    
    public static final Field local_field_946=new Field("local_field_946", "DG946-871");
    
    
    public static final Field local_field_947=new Field("local_field_947", "DG947-873");
    
    
    public static final Field local_field_948=new Field("local_field_948", "DG948-874");
    
    
    public static final Field local_field_949=new Field("local_field_949", "DG949-879");
    
    
    public static final Field local_field_951=new Field("local_field_951", "DPF note DBC");
    
    
    public static final Field local_field_997=new Field("local_field_997", "System field 997-B01");
    
    
    public static final Field local_field_998=new Field("local_field_998", "System field 998-BAS");
    
    
    public static final Field local_notes=new Field("local_notes", "Local Notes");
    
    
    public static final Field lom_context=new Field("lom_context", "lom-edu:context");
    
    
    public static final Field lom_description=new Field("lom_description", "lom-edu:description");
    
    
    public static final Field lom_difficulty=new Field("lom_difficulty", "lom-edu:difficulty");
    
    
    public static final Field lom_intendedEndUserRole=new Field("lom_intendedEndUserRole", "lom-edu:intendedEndUserRole");
    
    
    public static final Field lom_interactivityLevel=new Field("lom_interactivityLevel", "lom-edu:interactivityLevel");
    
    
    public static final Field lom_interactivityType=new Field("lom_interactivityType", "lom-edu:interactivityType");
    
    
    public static final Field lom_language=new Field("lom_language", "lom-edu:language");
    
    
    public static final Field lom_semanticDensity=new Field("lom_semanticDensity", "lom-edu:semanticDensity");
    
    
    public static final Field lom_type=new Field("lom_type", "lom-edu:type");
    
    
    public static final Field lom_typicalLearningTime=new Field("lom_typicalLearningTime", "lom-edu:typicalLearningTime");
    
    
    public static final SortableField main_pub_date=new SortableField("main_pub_date", "Publication Year");
    
    
    public static final Field media_type_code=new Field("media_type_code", "Media Type Code (Title)");
    
    
    public static final Field media_type_term=new Field("media_type_term", "Media Type Term (Title)");
    
    
    public static final Field medium_type=new Field("medium_type", "Medium Type");
    
    
    public static final Field mesh=new Field("mesh", "Medical Subjects (MeSH)");
    
    
    public static final Field mms_briefLevel=new Field("mms_briefLevel", "Brief Level");
    
    
    public static final Field mms_catalogerLevel=new Field("mms_catalogerLevel", "Cataloger Level");
    
    
    public static final Field mms_contributedBy=new Field("mms_contributedBy", "Contributed By");
    
    
    public static final Field mms_createDate=new Field("mms_createDate", "MMS Creation Date (Title)");
    
    
    public static final Field mms_id=new Field("mms_id", "MMS ID");
    
    
    public static final Field mms_managed_by=new Field("mms_managed_by", "Managed By Provider (Title)");
    
    
    public static final Field mms_material_type=new Field("mms_material_type", "Material type");
    
    
    public static final Field mms_mdImportModificationHistory=new Field("mms_mdImportModificationHistory", "Md Import modification job id");
    
    
    public static final Field mms_memberOfDeep=new Field("mms_memberOfDeep", "Collection ID");
    
    
    public static final Field mms_modificationDate=new Field("mms_modificationDate", "Modification Date (Title)");
    
    
    public static final Field mms_originatingSystem=new Field("mms_originatingSystem", "Originating System");
    
    
    public static final Field mms_originatingSystemId=new Field("mms_originatingSystemId", "Originating System Id");
    
    
    public static final Field mms_resource_type=new Field("mms_resource_type", "Resource Type");
    
    
    public static final Field mms_sip_id=new Field("mms_sip_id", "MMS SIP ID");
    
    
    public static final Field mms_tagSuppressed=new Field("mms_tagSuppressed", "Tag Suppressed (Title)");
    
    
    public static final Field mms_tagSuppressedExternalSearch=new Field("mms_tagSuppressedExternalSearch", "Tag Suppressed From External Search");
    
    
    public static final Field mms_tagSyncExternalCatalog=new Field("mms_tagSyncExternalCatalog", "Tag Sync External Catalog");
    
    
    public static final Field mms_tagSyncNationalCatalog=new Field("mms_tagSyncNationalCatalog", "Tag Sync External Catalog");
    
    
    public static final Field name=new Field("name", "Names");
    
    
    public static final Field national_bibliography_number=new Field("national_bibliography_number", "National Bibliography Number");
    
    
    public static final Field nlm_call_number=new Field("nlm_call_number", "NLM-type Call Number");
    
    
    public static final Field notes=new Field("notes", "Notes");
    
    
    public static final Field oclc_control_number_019_a=new Field("oclc_control_number_019_a", "OCLC Control Number (019)");
    
    
    public static final Field oclc_control_number_035_a=new Field("oclc_control_number_035_a", "OCLC Control Number (035a)");
    
    
    public static final Field oclc_control_number_035_az=new Field("oclc_control_number_035_az", "OCLC Control Number (035a+z)");
    
    
    public static final Field oclc_control_number_035_z=new Field("oclc_control_number_035_z", "OCLC Control Number (035z)");
    
    
    public static final Field open_access=new Field("open_access", "Open Access");
    
    
    public static final Field orcid_identifier=new Field("orcid_identifier", "ORCID - Open Researcher and Contributor ID");
    
    
    public static final Field original_cataloging_agency=new Field("original_cataloging_agency", "Original Cataloging Agency");
    
    
    public static final Field other_class_number=new Field("other_class_number", "Other Classification Number");
    
    
    public static final Field other_physical_details=new Field("other_physical_details", "Other Physical Details");
    
    
    public static final Field other_standard_identifier=new Field("other_standard_identifier", "Other Standard Identifier");
    
    
    public static final Field other_system_number=new Field("other_system_number", "Other System Number");
    
    
    public static final Field peer_reviewed=new Field("peer_reviewed", "Peer Reviewed");
    
    
    public static final Field publisher=new Field("publisher", "Publisher");
    
    
    public static final Field publisher_location=new Field("publisher_location", "Publisher Location");
    
    
    public static final Field publisher_number=new Field("publisher_number", "Publisher Number");
    
    
    public static final Field registry_id=new Field("registry_id", "Record Format");
    
    
    public static final Field relator=new Field("relator", "Relator");
    
    
    public static final Field series=new Field("series", "Series");
    
    
    public static final Field shelving_location=new Field("shelving_location", "Shelving Location");
    
    
    public static final Field source_record_id=new Field("source_record_id", "Source Record Id");
    
    
    public static final Field standard_number=new Field("standard_number", "Standard Number");
    
    
    public static final Field stored_standard_number_024_a_ind1_0=new Field("stored_standard_number_024_a_ind1_0", "International Standard Recording Code");
    
    
    public static final Field stored_standard_number_024_a_ind1_1=new Field("stored_standard_number_024_a_ind1_1", "Universal Product Code");
    
    
    public static final Field stored_standard_number_024_a_ind1_2=new Field("stored_standard_number_024_a_ind1_2", "International Standard Music Number");
    
    
    public static final Field stored_standard_number_024_a_ind1_3=new Field("stored_standard_number_024_a_ind1_3", "International Article Number");
    
    
    public static final Field stored_standard_number_024_a_ind1_4=new Field("stored_standard_number_024_a_ind1_4", "Serial Item and Contribution Identifier");
    
    
    public static final Field subject_category_code=new Field("subject_category_code", "Subject Category Code");
    
    
    public static final Field subjects=new Field("subjects", "Subjects");
    
    
    public static final Field sublocation=new Field("sublocation", "Sublocation");
    
    
    public static final SortableField title=new SortableField("title", "Title");
    
    
    public static final Field type_of_date=new Field("type_of_date", "Date Type Status");
    
    
    public static final Field type_of_record=new Field("type_of_record", "Bibliographic Format");
    
    
    public static final Field uniform_title=new Field("uniform_title", "Uniform Title");
    
    
    public static final Field unique_serial_title=new Field("unique_serial_title", "Serial Title");
    
    
    public static final Field universal_decimal_class_number=new Field("universal_decimal_class_number", "UDC");
    
    
    public static final Field alterCallNumber=new Field("alterCallNumber", "Item call number");
    
    
    public static final Field alternativeCallNumberType=new Field("alternativeCallNumberType", "Item call number type");
    
    
    public static final Field arrivalDate=new Field("arrivalDate", "Receiving date");
    
    
    public static final Field barcode=new Field("barcode", "Barcode");
    
    
    public static final Field baseStatus=new Field("baseStatus", "Base status");
    
    
    public static final Field chronologyI=new Field("chronologyI", "Chronology I");
    
    
    public static final Field chronologyJ=new Field("chronologyJ", "Chronology J");
    
    
    public static final Field chronologyK=new Field("chronologyK", "Chronology K");
    
    
    public static final Field chronologyL=new Field("chronologyL", "Chronology L");
    
    
    public static final Field chronologyM=new Field("chronologyM", "Chronology M");
    
    
    public static final Field current_Library=new Field("current_Library", "c.search.index_names.current_library");
    
    
    public static final Field current_Location=new Field("current_Location", "c.search.index_names.current_location");
    
    
    public static final Field enumerationA=new Field("enumerationA", "Enumeration A");
    
    
    public static final Field enumerationB=new Field("enumerationB", "Enumeration B");
    
    
    public static final Field enumerationC=new Field("enumerationC", "Enumeration C");
    
    
    public static final Field enumerationD=new Field("enumerationD", "Enumeration D");
    
    
    public static final Field enumerationE=new Field("enumerationE", "Enumeration E");
    
    
    public static final Field enumerationF=new Field("enumerationF", "Enumeration F");
    
    
    public static final Field enumerationG=new Field("enumerationG", "Enumeration G");
    
    
    public static final Field enumerationH=new Field("enumerationH", "Enumeration H");
    
    
    public static final Field expectedArrivalDate=new Field("expectedArrivalDate", "Expected receiving date");
    
    
    public static final Field expectedReturnFromTempLocation=new Field("expectedReturnFromTempLocation", "Due back from temp location date");
    
    
    public static final Field fulfilment_note=new Field("fulfilment_note", "Fulfillment Note");
    
    
    public static final Field internal_note_1=new Field("internal_note_1", "Internal note 1");
    
    
    public static final Field internal_note_2=new Field("internal_note_2", "Internal note 2");
    
    
    public static final Field internal_note_3=new Field("internal_note_3", "Internal note 3");
    
    
    public static final Field inventoryDate=new Field("inventoryDate", "Inventory date");
    
    
    public static final Field inventoryNumber=new Field("inventoryNumber", "Inventory number");
    
    
    public static final Field isMagnetic=new Field("isMagnetic", "Is Magnetic");
    
    
    public static final Field issueYear=new Field("issueYear", "Issue year");
    
    
    public static final Field item_createDate=new Field("item_createDate", "Creation Date (Physical Item)");
    
    
    public static final Field item_description=new Field("item_description", "Description");
    
    
    public static final Field item_modificationDate=new Field("item_modificationDate", "Modification Date (Physical Item)");
    
    
    public static final Field item_pid=new Field("item_pid", "Item PID");
    
    
    public static final Field itemIssueDate=new Field("itemIssueDate", "Item Issue Date");
    
    
    public static final Field itemPolicy=new Field("itemPolicy", "Item policy");
    
    
    public static final Field itemPOLineID=new Field("itemPOLineID", "PO Line");
    
    
    public static final Field itemSequenceNumber=new Field("itemSequenceNumber", "Item sequence number");
    
    
    public static final Field materialType=new Field("materialType", "Material Type (Physical Item)");
    
    
    public static final Field onShelfDate=new Field("onShelfDate", "On shelf date");
    
    
    public static final Field onShelfSeq=new Field("onShelfSeq", "On shelf seq");
    
    
    public static final Field pages=new Field("pages", "Pages");
    
    
    public static final Field physicalCondition=new Field("physicalCondition", "Physical condition");
    
    
    public static final Field pieces=new Field("pieces", "Pieces");
    
    
    public static final Field processType=new Field("processType", "Process type");
    
    
    public static final Field provenanceCode=new Field("provenanceCode", "Provenance Code");
    
    
    public static final Field public_note=new Field("public_note", "Public note (Physical Item)");
    
    
    public static final Field statisticsNote1=new Field("statisticsNote1", "Statistics note 1");
    
    
    public static final Field statisticsNote2=new Field("statisticsNote2", "Statistics note 2");
    
    
    public static final Field statisticsNote3=new Field("statisticsNote3", "Statistics note 3");
    
    
    public static final Field storageLocationID=new Field("storageLocationID", "Storage Location ID");
    
    
    public static final Field temporaryCallNumber=new Field("temporaryCallNumber", "Temporary call number");
    
    
    public static final Field temporaryCallNumberType=new Field("temporaryCallNumberType", "Temporary call number type");
    
    
    public static final Field temporaryItemPolicy=new Field("temporaryItemPolicy", "Temporary item policy");
    
    
    public static final Field temporaryLibrary=new Field("temporaryLibrary", "Temporary library");
    
    
    public static final Field temporaryPhysicalLocation=new Field("temporaryPhysicalLocation", "Temporary physical location");
    
    
    public static final Field temporaryPhysicalLocationInUse=new Field("temporaryPhysicalLocationInUse", "In temporary location");
    
    
    public static final Field accessionNumber=new Field("accessionNumber", "Accession Number");
    
    
    public static final Field acquisition_note=new Field("acquisition_note", "Acquisition Note");
    
    
    public static final Field action_note=new Field("action_note", "Action note");
    
    
    public static final Field binding_note=new Field("binding_note", "Binding Note");
    
    
    public static final Field callNumberPrefix=new Field("callNumberPrefix", "Call number prefix");
    
    
    public static final Field callNumberSuffix=new Field("callNumberSuffix", "Call number suffix");
    
    
    public static final Field f561a=new Field("f561a", "Ownership and Custodial History");
    
    
    public static final Field general_retention_policy=new Field("general_retention_policy", "General retention policy");
    
    
    public static final Field holding_carrier_type_code=new Field("holding_carrier_type_code", "Carrier Type Code (Title)");
    
    
    public static final Field holding_carrier_type_term=new Field("holding_carrier_type_term", "Carrier Type Term (Title)");
    
    
    public static final Field holding_Library=new Field("holding_Library", "Library (Holdings)");
    
    
    public static final Field holding_media_type_code=new Field("holding_media_type_code", "Media type code (Holdings)");
    
    
    public static final Field holding_media_type_term=new Field("holding_media_type_term", "Media type term (Holdings)");
    
    
    public static final Field holding_Note=new Field("holding_Note", "Holdings note");
    
    
    public static final Field holding_pid=new Field("holding_pid", "Holdings PID");
    
    
    public static final Field holding_tagSuppressed=new Field("holding_tagSuppressed", "Tag Suppressed (Holdings)");
    
    
    public static final Field lending_policy=new Field("lending_policy", "Lending Policy");
    
    
    public static final Field local_holding_field_984=new Field("local_holding_field_984", "Local holding field 984");
    
    
    public static final Field p_level_4_id=new Field("p_level_4_id", "c.search.index_names.has_items");
    
    
    public static final Field PermanentCallNumber=new Field("PermanentCallNumber", "Permanent call number");
    
    
    public static final Field permanentCallNumberType=new Field("permanentCallNumberType", "Permanent call number type");
    
    
    public static final Field permanentPhysicalLocation=new Field("permanentPhysicalLocation", "Permanent physical location");
    
    
    public static final Field reproduction_policy=new Field("reproduction_policy", "Reproduction Policy");
    
    
    public static final Field summaryHolding=new Field("summaryHolding", "Summary holdings");
    
    
    public static final Field available_for=new Field("available_for", "Available for");
    
    
    public static final Field available_for_group=new Field("available_for_group", "Available for group");
    
    
    public static final Field available_only_for=new Field("available_only_for", "Available for");
    
    
    public static final Field available_only_for_group=new Field("available_only_for_group", "Available only for group");
    
    
    public static final Field coverageInUse=new Field("coverageInUse", "Coverage In Use");
    
    
    public static final Field generalNote=new Field("generalNote", "Public note (Electronic Portfolio)");
    
    
    public static final Field is_standalone=new Field("is_standalone", "c.search.index_names.isStandalone");
    
    
    public static final Field portfolio_accessRights=new Field("portfolio_accessRights", "Access Rights (Electronic Portfolio)");
    
    
    public static final Field portfolio_accessType=new Field("portfolio_accessType", "Portfolio Access Type");
    
    
    public static final Field portfolio_activationDate=new Field("portfolio_activationDate", "Activation Date (Electronic Portfolio)");
    
    
    public static final Field portfolio_authenticationNote=new Field("portfolio_authenticationNote", "Authentication note (Electronic Portfolio)");
    
    
    public static final Field portfolio_baseStatus=new Field("portfolio_baseStatus", "Availability (Electronic Portfolio)");
    
    
    public static final Field portfolio_contributedBy=new Field("portfolio_contributedBy", "Contributed By (Electronic Portfolio)");
    
    
    public static final Field portfolio_creation_date=new Field("portfolio_creation_date", "Creation Date (Electronic Portfolio)");
    
    
    public static final Field portfolio_has_local_coverage=new Field("portfolio_has_local_coverage", "Has Local Coverage Information");
    
    
    public static final Field portfolio_interfaceName=new Field("portfolio_interfaceName", "Interface Name (Electronic Portfolio)");
    
    
    public static final Field portfolio_internalDescription=new Field("portfolio_internalDescription", "Internal Description (Electronic Portfolio)");
    
    
    public static final Field portfolio_is_local=new Field("portfolio_is_local", "Is Local (Electronic Portfolio)");
    
    
    public static final Field portfolio_Library=new Field("portfolio_Library", "c.search.index_names.portfolioLibrary");
    
    
    public static final Field portfolio_license_id=new Field("portfolio_license_id", "Portfolio License ID");
    
    
    public static final Field portfolio_managed_by=new Field("portfolio_managed_by", "Managed by Provider (Electronic Portfolio)");
    
    
    public static final Field portfolio_materialType=new Field("portfolio_materialType", "Material Type (Electronic Portfolio)");
    
    
    public static final Field portfolio_modification_date=new Field("portfolio_modification_date", "Modification Date (Electronic Portfolio)");
    
    
    public static final Field portfolio_note_out=new Field("portfolio_note_out", "Notes tab");
    
    
    public static final Field portfolio_pdaId=new Field("portfolio_pdaId", "Portfolio PDA ID");
    
    
    public static final Field portfolio_pid=new Field("portfolio_pid", "Portfolio PID");
    
    
    public static final Field portfolio_POLineID=new Field("portfolio_POLineID", "Portfolio PO Line ID");
    
    
    public static final Field portfolioProxyEnabled=new Field("portfolioProxyEnabled", "Proxy Enabled (Electronic Portfolio)");
    
    
    public static final Field proxy=new Field("proxy", "Proxy name (Electronic Portfolio)");
    
    
    public static final Field publicAccessModel=new Field("publicAccessModel", "Public Access Model");
    
    
    public static final Field purchaseModel=new Field("purchaseModel", "ProQuest purchase model");
    
    
    public static final Field url=new Field("url", "URL (Electronic Portfolio)");
    
    
    public static final Field ar_policy=new Field("ar_policy", "AR policy name");
    
    
    public static final Field date=new Field("date", "Date");
    
    
    public static final Field deposit_id=new Field("deposit_id", "Deposit ID");
    
    
    public static final Field digitization_request_id=new Field("digitization_request_id", "Request ID");
    
    
    public static final Field issue=new Field("issue", "Issue");
    
    
    public static final Field number=new Field("number", "Number");
    
    
    public static final Field preservationType=new Field("preservationType", "Usage Type");
    
    
    public static final Field remote_representation_integration_system_code=new Field("remote_representation_integration_system_code", "Remote repository");
    
    
    public static final Field remote_representation_linkingparameter1=new Field("remote_representation_linkingparameter1", "Linking Parameter 1");
    
    
    public static final Field remote_representation_linkingparameter2=new Field("remote_representation_linkingparameter2", "Linking Parameter 2");
    
    
    public static final Field remote_representation_linkingparameter3=new Field("remote_representation_linkingparameter3", "Linking Parameter 3");
    
    
    public static final Field remote_representation_linkingparameter4=new Field("remote_representation_linkingparameter4", "Linking Parameter 4");
    
    
    public static final Field remote_representation_linkingparameter5=new Field("remote_representation_linkingparameter5", "Linking Parameter 5");
    
    
    public static final Field remote_representation_originating_system_id=new Field("remote_representation_originating_system_id", "Originating Record ID");
    
    
    public static final Field rep_Library=new Field("rep_Library", "Library");
    
    
    public static final Field rep_public_note=new Field("rep_public_note", "Public note (Physical Item)");
    
    
    public static final Field repCreator=new Field("repCreator", "Creator (Title)");
    
    
    public static final Field representation_label=new Field("representation_label", "Representation Label");
    
    
    public static final Field representation_originating_system_id=new Field("representation_originating_system_id", "Provenance ID");
    
    
    public static final Field representation_pid=new Field("representation_pid", "Representation PID");
    
    
    public static final Field representationEntityType=new Field("representationEntityType", "Entity type");
    
    
    public static final Field repTitle=new Field("repTitle", "Title");
    
    
    public static final Field tagActive=new Field("tagActive", "Tag Active (Representation)");
    
    
    public static final Field volume=new Field("volume", "Volume");
    
    
    public static final Field activateFrom=new Field("activateFrom", "Activate From");
    
    
    public static final Field activateTo=new Field("activateTo", "Activate To");
    
    
    public static final Field aggregator=new Field("aggregator", "Electronic Collection Type");
    
    
    public static final Field category=new Field("category", "Category");
    
    
    public static final Field coll_available_for_group=new Field("coll_available_for_group", "Collection Available for group");
    
    
    public static final Field coll_available_only_for_group=new Field("coll_available_only_for_group", "Collection Available only for group");
    
    
    public static final Field creatorName=new Field("creatorName", "Creator Name (Electronic Collection)");
    
    
    public static final Field crossrefenabled=new Field("crossrefenabled", "Crossref enabled");
    
    
    public static final Field iepa_accessType=new Field("iepa_accessType", "Collection Access Type");
    
    
    public static final Field iepa_activationDate=new Field("iepa_activationDate", "Activation Date (Electronic Collection)");
    
    
    public static final Field iepa_authenticationNote=new Field("iepa_authenticationNote", "Authentication note (Electronic Collection)");
    
    
    public static final Field iepa_cdi_fulltext_rights=new Field("iepa_cdi_fulltext_rights", "CDI Fulltext rights");
    
    
    public static final Field iepa_cdi_linking=new Field("iepa_cdi_linking", "CDI Fulltext Linking");
    
    
    public static final Field iepa_cdi_newspaper_search=new Field("iepa_cdi_newspaper_search", "CDI Newspapers");
    
    
    public static final Field iepa_cdi_search_rights=new Field("iepa_cdi_search_rights", "CDI Search rights");
    
    
    public static final Field iepa_contributedBy=new Field("iepa_contributedBy", "Contributed By (Electronic Collection)");
    
    
    public static final Field iepa_in_cdi=new Field("iepa_in_cdi", "In CDI");
    
    
    public static final Field iepa_is_local=new Field("iepa_is_local", "Is Local (Electronic Collection)");
    
    
    public static final Field iepa_isFree=new Field("iepa_isFree", "Free (Electronic Collection)");
    
    
    public static final Field iepa_language=new Field("iepa_language", "Language (Electronic Collection)");
    
    
    public static final Field iepa_Library=new Field("iepa_Library", "c.search.index_names.iepaLibrary");
    
    
    public static final Field iepa_pid=new Field("iepa_pid", "Electronic Collection PID");
    
    
    public static final Field iepa_POLineID=new Field("iepa_POLineID", "Collection PO Line ID");
    
    
    public static final Field iepa_proxyEnabled=new Field("iepa_proxyEnabled", "Proxy Enabled (Electronic Collection)");
    
    
    public static final Field interfaceName=new Field("interfaceName", "Interface Name (Electronic Collection)");
    
    
    public static final Field internalDescription=new Field("internalDescription", "Internal Description (Electronic Collection)");
    
    
    public static final Field isFree=new Field("isFree", "Free (Service)");
    
    
    public static final Field linkingLevel=new Field("linkingLevel", "Linking Level");
    
    
    public static final Field linkResolverPlugin=new Field("linkResolverPlugin", "Link Resolver Plugin");
    
    
    public static final Field package_available_for=new Field("package_available_for", "Available for");
    
    
    public static final Field package_available_only_for=new Field("package_available_only_for", "Available for");
    
    
    public static final Field package_creation_date=new Field("package_creation_date", "Creation Date (Electronic Collection)");
    
    
    public static final Field package_license_id=new Field("package_license_id", "Service Authentication Note");
    
    
    public static final Field package_modification_date=new Field("package_modification_date", "Modification Date (Electronic Collection)");
    
    
    public static final Field package_nativeInterfaceUrl=new Field("package_nativeInterfaceUrl", "URL (Electronic Collection)");
    
    
    public static final Field packageName=new Field("packageName", "Electronic Collection Name");
    
    
    public static final Field pps_authenticationNote=new Field("pps_authenticationNote", "Service Authentication Note");
    
    
    public static final Field pps_baseStatus=new Field("pps_baseStatus", "Availability (Electronic Collection)");
    
    
    public static final Field proxySelected=new Field("proxySelected", "Proxy name (Electronic Collection)");
    
    
    public static final Field publicNote=new Field("publicNote", "Public note (Electronic Collection)");
    
    
    public static final Field service_pid=new Field("service_pid", "Title Service PID");
    
    
    public static final Field service_proxyEnabled=new Field("service_proxyEnabled", "Proxy Enabled (Service)");
    
    
    public static final Field serviceType=new Field("serviceType", "Service Type");
    
    
    public static final Field tps_managed_by=new Field("tps_managed_by", "Managed by Provider (Electronic Collection)");
    
    
    @Override
    public String toString() {
        return build();
    }
}
