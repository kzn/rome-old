package com.sun.syndication.io.impl;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.WireFeedGenerator;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Parent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Alejandro Abdelnur
 */
public abstract class BaseWireFeedGenerator implements WireFeedGenerator {

    /**
     * [TYPE].feed.ModuleParser.classes=  [className] ...
     */
    private static final String FEED_MODULE_GENERATORS_POSFIX_KEY = ".feed.ModuleGenerator.classes";

    /**
     * [TYPE].item.ModuleParser.classes= [className] ...
     */
    private static final String ITEM_MODULE_GENERATORS_POSFIX_KEY = ".item.ModuleGenerator.classes";

    /**
     * [TYPE].person.ModuleParser.classes= [className] ...
     */
    private static final String PERSON_MODULE_GENERATORS_POSFIX_KEY = ".person.ModuleGenerator.classes";


    private String _type;
    private ModuleGenerators _feedModuleGenerators;
    private ModuleGenerators _itemModuleGenerators;
    private ModuleGenerators _personModuleGenerators;
    private Namespace[] _allModuleNamespaces;

    protected BaseWireFeedGenerator(String type) {
        _type = type;
        _feedModuleGenerators = new ModuleGenerators(type + FEED_MODULE_GENERATORS_POSFIX_KEY, this);
        _itemModuleGenerators = new ModuleGenerators(type + ITEM_MODULE_GENERATORS_POSFIX_KEY, this);
        _personModuleGenerators = new ModuleGenerators(type + PERSON_MODULE_GENERATORS_POSFIX_KEY, this);
        Set<Namespace> allModuleNamespaces = new HashSet<Namespace>();
        Iterator<Namespace> i = _feedModuleGenerators.getAllNamespaces().iterator();
        while (i.hasNext()) {
            allModuleNamespaces.add(i.next());
        }
        i = _itemModuleGenerators.getAllNamespaces().iterator();
        while (i.hasNext()) {
            allModuleNamespaces.add(i.next());
        }
        i = _personModuleGenerators.getAllNamespaces().iterator();
        while (i.hasNext()) {
            allModuleNamespaces.add(i.next());
        }
        _allModuleNamespaces = new Namespace[allModuleNamespaces.size()];
        allModuleNamespaces.toArray(_allModuleNamespaces);
    }

    public String getType() {
        return _type;
    }

    protected void generateModuleNamespaceDefs(Element root) {
        for (int i = 0; i < _allModuleNamespaces.length; i++) {
            root.addNamespaceDeclaration(_allModuleNamespaces[i]);
        }
    }

    protected void generateFeedModules(List<Module> modules, Element feed) {
        _feedModuleGenerators.generateModules(modules, feed);
    }

    public void generateItemModules(List<Module> modules, Element item) {
        _itemModuleGenerators.generateModules(modules, item);
    }

    public void generatePersonModules(List<Module> modules, Element person) {
        _personModuleGenerators.generateModules(modules, person);
    }

    protected void generateForeignMarkup(Element e, List<Element> foreignMarkup) {
        if (foreignMarkup != null) {
            Iterator<Element> elems = (Iterator<Element>) foreignMarkup.iterator();
            while (elems.hasNext()) {
                Element elem = elems.next();
                Parent parent = elem.getParent();
                if (parent != null) {
                    parent.removeContent(elem);
                }
                e.addContent(elem);
            }
        }
    }

    /**
     * Purging unused declarations is less optimal, performance-wise, than never adding them in the first place.  So, we
     * should still ask the ROME guys to fix their code (not adding dozens of unnecessary module declarations). Having
     * said that: purging them here, before XML generation, is more efficient than parsing and re-molding the XML after
     * ROME generates it.
     * <p/>
     * Note that the calling app could still add declarations/modules to the Feed tree after this.  Which is fine.  But
     * those modules are then responsible for crawling to the root of the tree, at generate() time, to make sure their
     * namespace declarations are present.
     */
    protected static void purgeUnusedNamespaceDeclarations(Element root) {
        java.util.Set<String> usedPrefixes = new java.util.HashSet<String>();
        collectUsedPrefixes(root, usedPrefixes);

        List<Namespace> list = root.getAdditionalNamespaces();
        List<Namespace> additionalNamespaces = new java.util.ArrayList<Namespace>();
        additionalNamespaces.addAll(list); // the duplication will prevent a ConcurrentModificationException below

        for (int i = 0; i < additionalNamespaces.size(); i++) {
            Namespace ns = additionalNamespaces.get(i);
            String prefix = ns.getPrefix();
            if (prefix != null && prefix.length() > 0 && !usedPrefixes.contains(prefix)) {
                root.removeNamespaceDeclaration(ns);
            }
        }
    }

    private static void collectUsedPrefixes(Element el, java.util.Set<String> collector) {
        String prefix = el.getNamespacePrefix();
        if (prefix != null && prefix.length() > 0 && !collector.contains(prefix)) {
            collector.add(prefix);
        }
        List<Element> kids = el.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            collectUsedPrefixes(kids.get(i), collector); // recursion - worth it
        }
    }

}
