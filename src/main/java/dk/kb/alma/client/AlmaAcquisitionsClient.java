package dk.kb.alma.client;

import dk.kb.alma.gen.vendor.Account;
import dk.kb.alma.gen.vendor.Accounts;
import dk.kb.alma.gen.vendor.Addresses;
import dk.kb.alma.gen.vendor.ContactInfo;
import dk.kb.alma.gen.vendor.EdiInfo;
import dk.kb.alma.gen.vendor.Emails;
import dk.kb.alma.gen.vendor.Interfaces;
import dk.kb.alma.gen.vendor.Notes;
import dk.kb.alma.gen.vendor.PaymentMethods;
import dk.kb.alma.gen.vendor.Phones;
import dk.kb.alma.gen.vendor.Vendor;
import dk.kb.alma.gen.vendor.Vendors;
import dk.kb.alma.gen.vendor.WebAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlmaAcquisitionsClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    private final AlmaRestClient almaRestClient;
    
    public AlmaAcquisitionsClient(AlmaRestClient almaRestClient) {
        this.almaRestClient = almaRestClient;
    }


    public Vendors getVendors() {
        return getVendors("");
    }


    public Vendors getVendors(String searchString) {

        Vendors result = new Vendors();
        
        int limit = 100;
        int offset = 0;
        int hitCount = 1;
        while (true) {
            //TODO filter active?
            //TODO filter type?
            Vendors items = null;
            if (searchString.isEmpty()) {
                items = almaRestClient.get(almaRestClient.constructLink().path("/acq/vendors/")
                        .query("limit", limit)
                        .query("offset", offset), Vendors.class);
            } else {
                items = almaRestClient.get(almaRestClient.constructLink().path("/acq/vendors/")
                        .query("q", searchString)
                        .query("limit", limit)
                        .query("offset", offset), Vendors.class);

            }

            result.getVendors().addAll(items.getVendors());
            offset += items.getVendors().size();
            
            if (items.getVendors().size() != limit || offset >= items.getTotalRecordCount()) {
                break;
            }
        }
        result.setTotalRecordCount(result.getVendors().size());
        
        return result;
    }
    
    
    public Vendor getVendor(String code) {
        return almaRestClient.get(almaRestClient.constructLink().path("/acq/vendors/")
                                                .path(code), Vendor.class);
    }
    
    
    public Vendor updateVendor(Vendor vendor) {
        return almaRestClient.put(almaRestClient.constructLink().path("/acq/vendors/")
                                                .path(vendor.getCode()), Vendor.class, vendor);
    }
    
    public Vendor updateVendorAndChangeVendorCode(Vendor vendor, String oldVendorCode) {
        return almaRestClient.put(almaRestClient.constructLink().path("/acq/vendors/")
                                                .path(oldVendorCode), Vendor.class, vendor);
    }


    public Vendor createVendor(Vendor vendor) {
        return almaRestClient.post(almaRestClient.constructLink().path("/acq/vendors"), Vendor.class, vendor);
    }
    
    
    public void deleteVendor(String vendorCode) {
        almaRestClient.delete(almaRestClient.constructLink().path("/acq/vendors/")
                                            .path(vendorCode), Void.class);
    }
    
    
    public void deleteVendor(Vendor vendor) {
        almaRestClient.delete(almaRestClient.constructLink().path("/acq/vendors/")
                                            .path(vendor.getCode()), Void.class);
    }
    
    public Vendor newMaterialSupplierVendorObject(String vendorName,
                                                    String vendorCode,
                                                    String accountDescription,
                                                    String accountCode,
                                                  String paymentMethodCode) {
        Vendor vendor = new Vendor();
        vendor.setName(vendorName);
        vendor.setCode(vendorCode);
        
        Vendor.Status status = new Vendor.Status();
        vendor.setStatus(status);
        status.setValue("ACTIVE");
        status.setDesc("Active");
        
        Vendor.Language language = new Vendor.Language();
        vendor.setLanguage(language);
        language.setValue("da");
        language.setDesc("Danish");
        
        
        //List of currencies with which the vendor operates.
        //If not specified, all the institution's supported currencies will be considered as supported.
        /*
        Currencies currencies = new Currencies();
        vendor.setVendorCurrencies(currencies);
        Currencies.Currency currency = new Currencies.Currency();
        currencies.getCurrencies().add(currency);
        currency.setValue("ALL");
        currency.setDesc("Lek");
        */
        
        //List of libraries that the vendor works with.
        //If not specified, the default will be the institution including all sub units.
        /*
        Libraries libraries = new Libraries();
        vendor.setVendorLibraries(libraries);
        Library library = new Library();
        libraries.getLibraries().add(library);
        library.setIncludeSubUnits(Boolean.TRUE);
        Library.Code code = new Library.Code();
        library.setCode(code);
        code.setValue("45KBDK_KGL");
        code.setDesc("Royal Danish Library");
        */
        
        //List of the vendor's contacts information.
        //Possible INTERNAL_SERVER_ERROR if not set to at least this level
        ContactInfo contactInfo = new ContactInfo();
        vendor.setContactInfo(contactInfo);
        contactInfo.setAddresses(new Addresses());
        contactInfo.setEmails(new Emails());
        contactInfo.setPhones(new Phones());
        contactInfo.setWebAddresses(new WebAddresses());
        
        //vendor.setContactPersons(new ContactPersons());
        
        //The vendor's EDI related information.
        vendor.setEdiInfo(new EdiInfo());
        
        //At least one of the following fields must be true: material_supplier, access_provider, licensor, governmental.
        //Note that a vendor can be of few of the above type - for example, a vendor can be access provider and Licensor.
        
        //Since the XML object have Booleans, you MUST set the booleans or get INTERNAL_SERVER_ERROR
        
        
        // Material supplier/Subscription agent - Supplies the reading material or the subscription to the material
        vendor.setMaterialSupplier(Boolean.TRUE);
        //List of vendor related accounts.
        //For a material_supplier vendor, at least one active account must be defined.
        vendor.setAccounts(createAccount(accountDescription, accountCode, paymentMethodCode));
        
        // Access provider - Holds the access privileges to services which manage e-resources. Access is managed via interfaces.
        vendor.setAccessProvider(Boolean.FALSE);
        //List of the vendor's related interfaces.
        //Relevant only for an Access Provider vendor.
        //For an Access Provider vendor, at least one interface must be defined.
        //INTERNAL_SERVER_ERROR if this is null
        vendor.setInterfaces(new Interfaces());
        
        // Indicates that the vendor is to receive use tax (VAT) for an invoice payment.
        // The Governmental vendor's invoices handle only use tax on invoices,
        //   ensuring that tax payments are handled separately from the regular invoice charges and go directly to the government.
        // Use tax represents a tax on the usage of library items, and is expended from the same funds as the actual invoice charges.
        // Only a single Governmental vendor can be defined for an institution.
        vendor.setGovernmental(Boolean.FALSE);
        //Indication if invoices must be paid with VAT included.
        //For Governmental vendor, this field must be set to true in order to collect VAT for an invoice.
        vendor.setLiableForVat(Boolean.FALSE);
        
        //A numerical value for the tax to be administered.
        //Relevant only for governmental vendor.
        vendor.setTaxPercentage(null);
        
        
        // Licensor - Holds the license of electronic resources.
        vendor.setLicensor(Boolean.FALSE);
        
        vendor.setNotes(new Notes());
        return vendor;
    }
    
    
    private Accounts createAccount(String accountName, String accountCode, String paymentMethodCode) {
        Accounts accounts = new Accounts();
        Account account = new Account();
        accounts.getAccounts().add(account);
        account.setCode(accountCode);
        account.setDescription(accountName);
        
        Account.Status accountStatus = new Account.Status();
        account.setStatus(accountStatus);
        accountStatus.setValue("ACTIVE");
        accountStatus.setDesc("Active");
        
        /*
        Libraries accountlibraries = new Libraries();
        account.setAccountLibraries(accountlibraries);
        Library accountLibrary = new Library();
        accountlibraries.getLibraries().add(accountLibrary);
        accountLibrary.setIncludeSubUnits(Boolean.TRUE);
        Library.Code accountLibraryCode = new Library.Code();
        accountLibrary.setCode(accountLibraryCode);
        accountLibraryCode.setValue("45KBDK_KGL");
        accountLibraryCode.setDesc("Royal Danish Library");
        */

        
        //TODO is this nessesary?
        PaymentMethods paymentMethods = new PaymentMethods();
        account.setPaymentMethods(paymentMethods);
        PaymentMethods.PaymentMethod paymentMethod = new PaymentMethods.PaymentMethod();
        paymentMethods.getPaymentMethods().add(paymentMethod);
        paymentMethod.setValue(paymentMethodCode);
        //paymentMethod.setDesc("Accounting Department");
        
        //TODO are these nessesary?
        account.setSubscriptionInterval("90");
        account.setReclaimInterval("0");
    
        //Possible INTERNAL_SERVER_ERROR if not set to at least this level
        ContactInfo contactInfo = new ContactInfo();
        account.setContactInfo(contactInfo);
        contactInfo.setAddresses(new Addresses());
        contactInfo.setEmails(new Emails());
        contactInfo.setPhones(new Phones());
        
        return accounts;
    }

    /*******************
     * Purchaserequests
     ********************/

    public void deletePurchaseRequest(String purchaseId) {
        almaRestClient.delete(
                almaRestClient.constructLink()
                        .path("acq/purchase-requests/")
                        .path(purchaseId)
                , Void.class);
    }


}
