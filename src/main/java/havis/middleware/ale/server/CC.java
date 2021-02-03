package havis.middleware.ale.server;

import havis.middleware.ale.base.exception.DuplicateNameException;
import havis.middleware.ale.base.exception.DuplicateSubscriptionException;
import havis.middleware.ale.base.exception.ImplementationException;
import havis.middleware.ale.base.exception.InvalidURIException;
import havis.middleware.ale.base.exception.NoSuchNameException;
import havis.middleware.ale.base.exception.NoSuchSubscriberException;
import havis.middleware.ale.base.exception.ParameterException;
import havis.middleware.ale.base.exception.ParameterForbiddenException;
import havis.middleware.ale.base.exception.SecurityException;
import havis.middleware.ale.base.exception.ValidationException;
import havis.middleware.ale.core.report.cc.data.Associations;
import havis.middleware.ale.core.report.cc.data.Caches;
import havis.middleware.ale.core.report.cc.data.Randoms;
import havis.middleware.ale.core.security.Method;
import havis.middleware.ale.service.cc.AssocTableEntry;
import havis.middleware.ale.service.cc.AssocTableSpec;
import havis.middleware.ale.service.cc.CCParameterListEntry;
import havis.middleware.ale.service.cc.CCReports;
import havis.middleware.ale.service.cc.CCSpec;
import havis.middleware.ale.service.cc.EPCCacheSpec;
import havis.middleware.ale.service.cc.RNGSpec;

import java.util.List;

public class CC {

    private static Security security = Security.getInstance();
    private static CC instance = new CC();

    private CC() {
    }

    public static CC getInstance() {
        return instance;
    }

    private static havis.middleware.ale.core.manager.CC getManager() {
        return havis.middleware.ale.core.manager.CC.getInstance();
    }

    private static Caches getCaches() {
        return Caches.getInstance();
    }

    private static Associations getAssociations() {
        return Associations.getInstance();
    }

    private static Randoms getRandoms() {
        return Randoms.getInstance();
    }

    public void define(String name, CCSpec spec) throws ValidationException, DuplicateNameException, ImplementationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_define);
        getManager().define(name, spec, true);
    }

    public void undefine(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_undefine);
        getManager().undefine(name, true);
    }

    public CCSpec getSpec(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getCCSpec);
        return getManager().getSpec(name);
    }

    public java.util.List<String> getNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getCCSpecNames);
        return getManager().getNames();
    }

    public void subscribe(String name, String uri) throws DuplicateSubscriptionException, ImplementationException, InvalidURIException, NoSuchNameException,
            ParameterForbiddenException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_subscribe);
        getManager().subscribe(name, uri, null, true);
    }

    public void unsubscribe(String name, String uri) throws ImplementationException, InvalidURIException, NoSuchNameException, SecurityException,
            NoSuchSubscriberException {
        security.isAllowed(Method.ALECC, Method.ALECC_unsubscribe);
        getManager().unsubscribe(name, uri, true);
    }

    public CCReports poll(String name, List<CCParameterListEntry> entries) throws ImplementationException, NoSuchNameException, ParameterException,
            SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_poll);
        return getManager().poll(name, entries);
    }

    public CCReports immediate(CCSpec spec) throws ValidationException, ImplementationException, ParameterForbiddenException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_immediate);
        return getManager().immediate(spec);
    }

    public java.util.List<String> getSubscribers(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getSubscribers);
        return getManager().getSubscribers(name);
    }

    public String getStandardVersion() throws ImplementationException {
        return havis.middleware.ale.core.manager.CC.getStandardVersion();
    }

    public void defineEPCCache(String name, EPCCacheSpec spec, List<String> replenishment) throws DuplicateNameException,
            havis.middleware.ale.base.exception.EPCCacheSpecValidationException, ImplementationException,
            havis.middleware.ale.base.exception.InvalidPatternException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_defineEPCCache);
        getCaches().define(name, spec, replenishment, true);
    }

    public List<String> undefineEPCCache(String name) throws ImplementationException, havis.middleware.ale.base.exception.InUseException, NoSuchNameException,
            SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_undefine);
        return getCaches().undefine(name, true);
    }

    public EPCCacheSpec getEPCCache(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getEPCCache);
        return getCaches().getSpec(name);
    }

    public List<String> getEPCCacheNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getCCSpecNames);
        return getCaches().getNames();
    }

    public void replenishEPCCache(String name, List<String> replenishment) throws ImplementationException,
            havis.middleware.ale.base.exception.InvalidPatternException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_replenishEPCCache);
        getCaches().replenish(name, replenishment, true);
    }

    public List<String> depleteEPCCache(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_depleteEPCCache);
        return getCaches().deplete(name, true);
    }

    public List<String> getEPCCacheContents(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getEPCCacheContents);
        return getCaches().getContents(name);
    }

    public void defineAssocTable(String name, AssocTableSpec spec, List<AssocTableEntry> entries)
            throws havis.middleware.ale.base.exception.AssocTableValidationException, DuplicateNameException, ImplementationException,
            havis.middleware.ale.base.exception.InvalidAssocTableEntryException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_defineAssocTable);
        getAssociations().define(name, spec, entries, true);
    }

    public void undefineAssocTable(String name) throws ImplementationException, havis.middleware.ale.base.exception.InUseException, NoSuchNameException,
            SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_undefineAssocTable);
        getAssociations().undefine(name, true);
    }

    public List<String> getAssocTableNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getAssocTableNames);
        return getAssociations().getNames();
    }

    public AssocTableSpec getAssocTable(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getAssocTable);
        return getAssociations().getSpec(name);
    }

    public void putAssocTableEntries(String name, List<AssocTableEntry> entries) throws ImplementationException,
            havis.middleware.ale.base.exception.InvalidAssocTableEntryException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_putAssocTableEntries);
        getAssociations().putEntries(name, entries, true);
    }

    public String getAssocTableValue(String name, String epc) throws ImplementationException, havis.middleware.ale.base.exception.InvalidEPCException,
            NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getAssocTableValue);
        return getAssociations().getValue(name, epc);
    }

    public java.util.List<AssocTableEntry> getAssocTableEntries(String name, List<String> patterns) throws ImplementationException,
            havis.middleware.ale.base.exception.InvalidPatternException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getAssocTableEntries);
        return getAssociations().getEntries(name, patterns);
    }

    public void removeAssocTableEntry(String name, String epc) throws ImplementationException, havis.middleware.ale.base.exception.InvalidEPCException,
            NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_removeAssocTableEntry);
        getAssociations().removeEntry(name, epc, true);
    }

    public void removeAssocTableEntries(String name, List<String> patterns) throws ImplementationException,
            havis.middleware.ale.base.exception.InvalidPatternException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_removeAssocTableEntries);
        getAssociations().removeEntries(name, patterns, true);
    }

    public void defineRNG(String name, RNGSpec spec) throws DuplicateNameException, ImplementationException,
            havis.middleware.ale.base.exception.RNGValidationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_defineRNG);
        getRandoms().define(name, spec, true);
    }

    public void undefineRNG(String name) throws ImplementationException, havis.middleware.ale.base.exception.InUseException, NoSuchNameException,
            SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_undefineRNG);
        getRandoms().undefine(name, true);
    }

    public List<String> getRNGNames() throws ImplementationException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getRNGNames);
        return getRandoms().getNames();
    }

    public RNGSpec getRNG(String name) throws ImplementationException, NoSuchNameException, SecurityException {
        security.isAllowed(Method.ALECC, Method.ALECC_getRNG);
        return getRandoms().getSpec(name);
    }

    public void dispose() {
        getManager().dispose();
        getCaches().dispose();
        getAssociations().dispose();
        getRandoms().dispose();
    }
}