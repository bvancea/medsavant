package org.ut.biolab.medsavant.client.view.genetics.family;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ut.biolab.medsavant.MedSavantClient;
import org.ut.biolab.medsavant.client.geneset.GeneSetController;
import org.ut.biolab.medsavant.client.login.LoginController;
import org.ut.biolab.medsavant.client.patient.PedigreeFields;
import org.ut.biolab.medsavant.shared.model.Cohort;
import org.ut.biolab.medsavant.shared.model.Gene;
import org.ut.biolab.medsavant.shared.model.SimplePatient;
import org.ut.biolab.medsavant.client.project.ProjectController;
import org.ut.biolab.medsavant.shared.util.ChromosomeComparator;
import org.ut.biolab.medsavant.client.util.DataRetriever;
import org.ut.biolab.medsavant.client.util.MedSavantExceptionHandler;
import org.ut.biolab.medsavant.client.util.MedSavantWorker;
import org.ut.biolab.medsavant.client.view.Notification;
import org.ut.biolab.medsavant.client.view.component.SearchableTablePanel;
import org.ut.biolab.medsavant.client.view.component.StripyTable;
import org.ut.biolab.medsavant.client.view.genetics.family.FamilyMattersOptionView.IncludeExcludeStep;
import org.ut.biolab.medsavant.client.view.genetics.family.FamilyMattersOptionView.InheritanceStep;
import org.ut.biolab.medsavant.client.view.genetics.family.FamilyMattersOptionView.InheritanceStep.InheritanceModel;
import org.ut.biolab.medsavant.client.view.genetics.inspector.ComprehensiveInspector;
import org.ut.biolab.medsavant.client.view.genetics.variantinfo.SimpleVariant;
import org.ut.biolab.medsavant.client.view.util.ViewUtil;
import org.ut.biolab.medsavant.shared.model.SessionExpiredException;
import org.ut.biolab.medsavant.shared.vcf.VariantRecord;
import org.ut.biolab.medsavant.shared.vcf.VariantRecord.Zygosity;

/**
 *
 * @author mfiume
 */
public class FamilyMattersWorker extends MedSavantWorker<TreeMap<SimpleFamilyMattersVariant, FamilyMattersWorker.SimplePatientSet>> implements PedigreeFields {

    private static final Log LOG = LogFactory.getLog(FamilyMattersWorker.class);
    private final List<IncludeExcludeStep> steps;
    private final File inFile;
    //private JProgressBar progressBar;
    //private JFrame frame;
    //private JLabel label;
    private Notification jobModel;
    private Locks.DialogLock completionLock;
    private final InheritanceStep inheritanceStep;

    public FamilyMattersWorker(List<IncludeExcludeStep> steps, InheritanceStep model, File inFile) {
        super(FamilyMattersPage.PAGE_NAME);
        this.steps = steps;
        this.inheritanceStep = model;
        this.inFile = inFile;
    }

    public void setUIComponents(Notification m) {
        this.jobModel = m;
    }

    public void setLabelText(String t) {
        this.jobModel.setStatusMessage(t);
    }

    @Override
    protected void showProgress(double fract) {
        this.jobModel.setProgress(fract);
    }

    @Override
    protected void showSuccess(final TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> result) {

        setLabelText("Preparing results");

        String pageName = FamilyMattersPage.PAGE_NAME;
        String[] columnNames = new String[]{"Chromosome", "Position", "Reference", "Alternate", "Type", "Samples", "Genes"};
        Class[] columnClasses = new Class[]{String.class, String.class, String.class, String.class, String.class, String.class, String.class};

        final List<SimpleFamilyMattersVariant> variants = new ArrayList<SimpleFamilyMattersVariant>(result.keySet());

        LOG.info(variants.size() + " variants to be shown");

        DataRetriever<Object[]> retriever = new DataRetriever<Object[]>() {
            @Override
            public List<Object[]> retrieve(int start, int limit) throws Exception {

                LOG.info("Showing " + limit + " results starting from " + start);
                List<Object[]> rows = new ArrayList<Object[]>();

                for (int i = 0; i < limit && (i + start) < variants.size(); i++) {

                    SimpleFamilyMattersVariant v = variants.get(i + start);

                    if (i < 10) {
                        LOG.info(v);
                    }

                    String geneStr = "";
                    for (SimpleFamilyMattersGene g : v.getGenes()) {
                        geneStr += g.name + ", ";
                    }
                    if (v.getGenes().size() > 0) {
                        geneStr = geneStr.substring(0, geneStr.length() - 2);

                        rows.add(new Object[]{
                                    v.chr,
                                    v.pos,
                                    v.ref,
                                    v.alt,
                                    v.type,
                                    result.get(v).toString(),//.replaceAll("\"", ""),//.replaceFirst("\[", "").replaceFirst("\]", ""),
                                    geneStr});
                    }


                }

                return rows;
            }

            @Override
            public int getTotalNum() {
                LOG.info("Number of results " + result.keySet().size());
                return variants.size();
            }

            @Override
            public void retrievalComplete() {
            }
        };

        final SearchableTablePanel stp =
                new SearchableTablePanel(
                pageName,
                columnNames,
                columnClasses,
                new int[]{},
                true,
                false,
                500,
                true,
                SearchableTablePanel.TableSelectionType.ROW,
                1000,
                retriever);

        final ComprehensiveInspector vip = new ComprehensiveInspector(true, false, false, true, true, true, false);

        stp.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                if (e.getValueIsAdjusting()) {
                    return;
                }

                int index = stp.getActualRowAcrossAllPages(stp.getTable().getSelectedRow());//e.getLastIndex());

                SimpleFamilyMattersVariant fmv = variants.get(index);
                LOG.info("Selected " + stp.getTable().getSelectedRow() + " real row is " + index + " " + fmv);

                SimpleVariant v = new SimpleVariant(fmv.chr, fmv.pos, fmv.ref, fmv.alt, fmv.type);
                vip.setSimpleVariant(v);
            }
        });

        JDialog f = new JDialog();
        f.setTitle("Cohort Analysis Results");

        LOG.info("Showing table of results");

        JPanel aligned = new JPanel();
        aligned.setLayout(new BorderLayout());
        aligned.setBorder(null);
        aligned.setPreferredSize(new Dimension(450, 999));
        aligned.setBackground(Color.white);
        aligned.add(vip, BorderLayout.CENTER);

        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(stp, BorderLayout.CENTER);
        p.add(aligned, BorderLayout.EAST);

        f.add(p);

        stp.forceRefreshData();
        f.setPreferredSize(new Dimension(600, 600));
        f.setMinimumSize(new Dimension(600, 600));

        if (completionLock != null) {
            synchronized (completionLock) {
                completionLock.setResultsFrame(f);
                completionLock.notify();
            }
        }
    }

    void setCompletionLock(Locks.DialogLock lock) {
        this.completionLock = lock;
    }

    private void filterForCompoundHeterozygote(TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> variantToSampleMap, TreeMap<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>> genesToVariantsMap, List<SimpleFamily> families) {
        // a list of variants to eventually remove
        List<SimpleFamilyMattersVariant> variantsToRemove = new ArrayList<SimpleFamilyMattersVariant>();

        int counter = 0;
        int announceEvery = 100;

        int total = variantToSampleMap.keySet().size();

        // consider each variant in turn
        for (SimpleFamilyMattersVariant variant : variantToSampleMap.keySet()) {

            if (counter % announceEvery == 0) {
                double percent = ((double)counter)*100/total;
                LOG.info("Processed " + counter + " of " + variantToSampleMap.keySet().size() +  " (" + percent + "%) variants");
            }
            counter++;

            boolean passesForAllFamilies = true;

            // test the model on each family
            for (SimpleFamily fam : families) {

                boolean passesForFamily = false;

                for (SimpleFamilyMattersGene gene : variant.getGenes()) {

                    for (SimpleFamilyMattersVariant otherVariantInGene : genesToVariantsMap.get(gene)) {
                        if (variant == otherVariantInGene) {
                            continue;
                        }

                        passesForFamily = passesForFamily || testVariantsforCompountHeterozygousInFamily(variant, otherVariantInGene, variantToSampleMap.get(variant), variantToSampleMap.get(otherVariantInGene), fam);

                    }

                }

                passesForAllFamilies = passesForAllFamilies && passesForFamily;
            }

            // record the ones that don't pass
            if (!passesForAllFamilies) {
                variantsToRemove.add(variant);
            }
        }

        // remove the ones that don't pass
        for (SimpleFamilyMattersVariant v : variantsToRemove) {
            variantToSampleMap.remove(v);
        }
    }

    private boolean testVariantsforCompountHeterozygousInFamily(SimpleFamilyMattersVariant variant1, SimpleFamilyMattersVariant variant2, SimplePatientSet possessors1, SimplePatientSet possessors2, SimpleFamily fam) {

        if (possessors1 == null  || possessors2 == null) { return false; }

        boolean evidenceInFamily = false;

        //System.out.println("Comparing " + variant1 + " (" + possessors1 + ") to " + variant2 + " (" + possessors2 + ")");

        // remove otherwise non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            // affected, should have both as het
            if (p.isAffected()) {

                String dnaID = p.getDnaID();

                // affecteds should have variant entries at both sites
                if (!possessors1.containsDNAID(dnaID) || !possessors2.containsDNAID(dnaID)) {
                    return false;
                }

                Zygosity variant1ThisZygosity = possessors1.getZygosityForDNAID(dnaID);
                Zygosity variant2ThisZygosity = possessors2.getZygosityForDNAID(dnaID);

                // affecteds should be het at both sites
                if (variant1ThisZygosity != Zygosity.Hetero || variant2ThisZygosity != Zygosity.Hetero) {
                    return false;
                }

                // each parents should have one
                String momDNAId = p.getMomDNAID();
                String dadDNAId = p.getDadDNAID();
                if  (momDNAId != null && dadDNAId != null
                        && (
                            // both have first
                            (possessors1.containsDNAID(momDNAId) && possessors1.containsDNAID(dadDNAId))
                            // neither has first
                            || (!possessors1.containsDNAID(momDNAId) && !possessors1.containsDNAID(dadDNAId))
                            // both have second
                            || (possessors2.containsDNAID(momDNAId) && possessors2.containsDNAID(dadDNAId))
                            // neither has second
                            || (!possessors2.containsDNAID(momDNAId) && !possessors2.containsDNAID(dadDNAId))
                        )
                    ) {
                    return false;
                }

                if (p.areBothParentsLinked()) {

                    boolean momHasOne = possessors1.containsDNAID(momDNAId) || possessors2.containsDNAID(momDNAId);
                    boolean dadHasOne = possessors1.containsDNAID(dadDNAId) || possessors2.containsDNAID(dadDNAId);

                    // each has exactly one, since 0 and 2 count checks were done above
                    if (momHasOne && dadHasOne) {
                        evidenceInFamily = true;
                    }
                }
            } else {

                // should not be anything but het at these positions
                String dnaID = p.getDnaID();

                if (possessors1.containsDNAID(dnaID) && possessors1.getZygosityForDNAID(dnaID) != Zygosity.Hetero) { return false; }
                if (possessors2.containsDNAID(dnaID) && possessors2.getZygosityForDNAID(dnaID) != Zygosity.Hetero) { return false; }

            }
        }

        return evidenceInFamily;
    }

    protected class SimplePatientSet extends HashSet<SimplePatient> {

        private final HashMap<String, SimplePatient> dnaIDs;

        public SimplePatientSet() {
            super();
            this.dnaIDs = new HashMap<String, SimplePatient>();
        }

        public Collection<SimplePatient> getPatients() {
            return dnaIDs.values();
        }

        @Override
        public String toString() {
            String st = "";
            for (String s : dnaIDs.keySet()) {
                st += s + ", ";
            }
            return st;
        }


        @Override
        public boolean add(SimplePatient o) {
            this.dnaIDs.put(o.getDnaID(), o);
            return super.add(o);
        }

        public boolean containsDNAID(String dnaID) {
            return dnaIDs.containsKey(dnaID);
        }

        private SimplePatient getPatientGenotypeForDNAID(String dnaID) {
            return dnaIDs.get(dnaID);
        }

        private Zygosity getZygosityForDNAID(String dnaID) {
            return getPatientGenotypeForDNAID(dnaID).getZygosity();
        }
    }

    private void filterForSimpleModels(InheritanceModel model, TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> variantToSampleMap, List<SimpleFamily> families) {

        // a list of variants to eventually remove
        List<SimpleFamilyMattersVariant> variantsToRemove = new ArrayList<SimpleFamilyMattersVariant>();

        // consider each variant in turn
        for (SimpleFamilyMattersVariant variant : variantToSampleMap.keySet()) {
            boolean passesForAllFamilies = true;

            // test the model on each family
            for (SimpleFamily fam : families) {
                boolean passesForFamily = false;
                switch (model) {
                    case AUTOSOMAL_DOMINANT:
                        passesForFamily = testVariantforAutosmalDominantInFamily(variant, variantToSampleMap.get(variant), fam);
                        break;
                    case AUTOSOMAL_RECESSIVE:
                        passesForFamily = testVariantforAutosmalRecessiveInFamily(variant, variantToSampleMap.get(variant), fam);
                        break;
                    case X_LINKED_DOMINANT:
                        passesForFamily = testVariantforXLinkedDominantInFamily(variant, variantToSampleMap.get(variant), fam);
                        break;
                    case X_LINKED_RECESSIVE:
                        passesForFamily = testVariantforXLinkedRecessiveInFamily(variant, variantToSampleMap.get(variant), fam);
                        break;
                    case DE_NOVO:
                        passesForFamily = testVariantforDeNovoInFamily(variant, variantToSampleMap.get(variant), fam);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unkown model: " + model.toString());
                }
                passesForAllFamilies = passesForAllFamilies && passesForFamily;
            }

            // record the ones that don't pass
            if (!passesForAllFamilies) {
                variantsToRemove.add(variant);
            }
        }

        System.out.println("Done running model" + model);

        // remove the ones that don't pass
        for (SimpleFamilyMattersVariant v : variantsToRemove) {
            variantToSampleMap.remove(v);
        }
    }

    private boolean testVariantforDeNovoInFamily(SimpleFamilyMattersVariant variant, SimplePatientSet possessors, SimpleFamily fam) {

        boolean evidenceInFamily = false;

        // remove non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            // affected, parents should not have it
            if (p.isAffected()) {

                // someone who's affected in the family has it
                evidenceInFamily = true;

                // both parents shouldn't have it
                String momDNAId = p.getMomDNAID();
                String dadDNAId = p.getDadDNAID();
                if (momDNAId != null && possessors.containsDNAID(p.getMomDNAID())) {
                    return false;
                }
                if (dadDNAId != null && possessors.containsDNAID(p.getDadDNAID())) {
                    return false;
                }

            } else {

                // shouldnt have unaffected posessors
                if (possessors.containsDNAID(p.getDnaID())) {
                    return false;
                }
            }
        }

        return evidenceInFamily;
    }

    private boolean testVariantforXLinkedDominantInFamily(SimpleFamilyMattersVariant variant, SimplePatientSet possessors, SimpleFamily fam) {

        // remove non-x chrs
        String simpleChr = variant.chr.toLowerCase();
        if (!simpleChr.equals("chrX") && !simpleChr.equals("X")) {
            return false;
        }

        boolean evidenceInFamily = false;

        // remove otherwise non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            // all affected individuals should have it
            if (p.isAffected() != possessors.containsDNAID(p.getDnaID())) {
                return false;
            }

            if (p.isAffected() && possessors.containsDNAID(p.getDnaID())) {
                evidenceInFamily = true;
            }

            if (p.areBothParentsLinked()) {
                // one of the parents should have it
                if (!possessors.containsDNAID(p.getMomDNAID()) && !possessors.containsDNAID(p.dadDNAID)) {
                    return false;
                }
            }
        }
        return evidenceInFamily;
    }

    private boolean testVariantforXLinkedRecessiveInFamily(SimpleFamilyMattersVariant variant, SimplePatientSet possessors, SimpleFamily fam) {

        // remove non-x chrs
        String simpleChr = variant.chr.toLowerCase();
        if (!simpleChr.equals("chrX") && !simpleChr.equals("X")) {
            return false;
        }

        boolean evidenceInFamily = false;

        // remove otherwise non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            if (p.getGender() == Gender.FEMALE) {

                // affected, should have it and be homozygous, and in both parents
                if (p.isAffected()) {

                    // should have it
                    if (!possessors.containsDNAID(p.getDnaID())) {
                        return false;
                    }

                    Zygosity z = possessors.getZygosityForDNAID(p.getDnaID());

                    // should be homozygous
                    if (z != Zygosity.HomoAlt) { // assuming homoref isn't causal
                        return false;
                    }

                    // both parents should have it
                    String momDNAId = p.getMomDNAID();
                    String dadDNAId = p.getDadDNAID();
                    if (momDNAId != null && !possessors.containsDNAID(p.getMomDNAID())) {
                        return false;
                    }
                    if (dadDNAId != null && !possessors.containsDNAID(p.getDadDNAID())) {
                        return false;
                    }

                    if (p.areBothParentsLinked()) {
                        evidenceInFamily = true;
                    }

                } else {

                    // shouldnt have unaffected female homozygotes
                    if (possessors.containsDNAID(p.getDnaID()) && (possessors.getZygosityForDNAID(p.getDnaID()) == Zygosity.HomoAlt)) { // assuming homoref isn't causal
                        return false;
                    }
                }
            } else if (p.getGender() == Gender.MALE) {


                // affected, should have it and be heterozygous, and in one of the parents
                if (p.isAffected()) {

                    // should have it
                    if (!possessors.containsDNAID(p.getDnaID())) {
                        return false;
                    }

                    Zygosity z = possessors.getZygosityForDNAID(p.getDnaID());

                    // should be heterozygous
                    if (z != Zygosity.Hetero) {
                        return false;
                    }

                    // one parent should have it
                    String momDNAId = p.getMomDNAID();
                    String dadDNAId = p.getDadDNAID();

                    boolean atLeastOneParentHasIt = false;
                    if (momDNAId != null && possessors.containsDNAID(p.getMomDNAID())) {
                        atLeastOneParentHasIt = true;
                    }
                    if (dadDNAId != null && possessors.containsDNAID(p.getDadDNAID())) {
                        atLeastOneParentHasIt = true;
                    }

                    // if parents are linked
                    if (p.areBothParentsLinked()) {

                        // follows model
                        if (atLeastOneParentHasIt) {
                            evidenceInFamily = true;

                            // doesn't follow model, neither parent has it
                        } else {
                            return false;
                        }
                    }

                } else {

                    // shouldnt have unaffected heterozygous males
                    if (possessors.containsDNAID(p.getDnaID()) && (possessors.getZygosityForDNAID(p.getDnaID()) == Zygosity.Hetero)) {
                        return false;
                    }
                }
            }
        }

        return evidenceInFamily;
    }

    private boolean testVariantforAutosmalDominantInFamily(SimpleFamilyMattersVariant variant, SimplePatientSet possessors, SimpleFamily fam) {

        // remove sex chrs
        String simpleChr = variant.chr.toLowerCase();
        if (simpleChr.equals("chrX") || simpleChr.equals("X") || simpleChr.equals("chrY") || simpleChr.equals("Y")) {
            return false;
        }

        boolean evidenceInFamily = false;

        // remove otherwise non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            // all affected individuals should have it
            if (p.isAffected() != possessors.containsDNAID(p.getDnaID())) {
                return false;
            }

            if (p.isAffected() && possessors.containsDNAID(p.getDnaID())) {
                evidenceInFamily = true;
            }

            if (p.areBothParentsLinked()) {
                // one of the parents should have it
                if (!possessors.containsDNAID(p.getMomDNAID()) && !possessors.containsDNAID(p.dadDNAID)) {
                    return false;
                }
            }
        }
        return evidenceInFamily;
    }

    private boolean testVariantforAutosmalRecessiveInFamily(SimpleFamilyMattersVariant variant, SimplePatientSet possessors, SimpleFamily fam) {

        // remove sex chrs
        String simpleChr = variant.chr.toLowerCase();
        if (simpleChr.equals("chrX") || simpleChr.equals("X") || simpleChr.equals("chrY") || simpleChr.equals("Y")) {
            return false;
        }

        boolean evidenceInFamily = false;

        // remove otherwise non-compliant variants
        for (SimplePerson p : fam.getFamilyMembers()) {

            // affected, should have it and be homozygous, and in both parents
            if (p.isAffected()) {

                // should have it
                if (!possessors.containsDNAID(p.getDnaID())) {
                    //LOG.info(variant + " = F : affected but don't have this " + possessors);
                    return false;
                }

                Zygosity z = possessors.getZygosityForDNAID(p.getDnaID());

                // should be homozygous
                if (z != Zygosity.HomoAlt) { // assuming homoref isn't causal
                    //LOG.info(variant + " = F : affected but not homozygous " + possessors);
                    return false;
                }

                // both parents should have it
                String momDNAId = p.getMomDNAID();
                String dadDNAId = p.getDadDNAID();
                if (momDNAId != null && !possessors.containsDNAID(p.getMomDNAID())) {
                    //LOG.info(variant + " = F : mother doesn't have it " + possessors);
                    return false;
                }
                if (dadDNAId != null && !possessors.containsDNAID(p.getDadDNAID())) {
                    //LOG.info(variant + " = F : father doesn't have it " + possessors);
                    return false;
                }

                if (p.areBothParentsLinked()) {
                    evidenceInFamily = true;
                }

            } else {

                // shouldnt have unaffected homozygotes
                if (possessors.containsDNAID(p.getDnaID()) && (possessors.getZygosityForDNAID(p.getDnaID()) == Zygosity.HomoAlt)) { // assuming homoref isn't causal
                    //LOG.info(variant + " = F : there's an unaffected homozygote " + possessors);
                    return false;
                }
            }
        }

        return evidenceInFamily;
    }

    private static class VariantGeneComparator implements Comparator {

        private final boolean useGeneStart;
        private static final ChromosomeComparator cc = new ChromosomeComparator();

        public VariantGeneComparator(boolean useGeneStart) {
            this.useGeneStart = useGeneStart;
        }

        @Override
        public int compare(Object v, Object g) {
            if (v instanceof SimpleFamilyMattersVariant && g instanceof SimpleFamilyMattersGene) {

                SimpleFamilyMattersVariant v0 = (SimpleFamilyMattersVariant) v;
                SimpleFamilyMattersGene g0 = (SimpleFamilyMattersGene) g;
                int c = cc.compare(v0.chr, g0.chr);
                if (c == 0) {
                    if (useGeneStart) {
                        return v0.pos < g0.start ? -1 : v0.pos == g0.start ? 0 : 1;
                    } else {
                        return v0.pos < g0.end ? -1 : v0.pos == g0.end ? 0 : 1;
                    }
                } else {
                    return c;
                }
            } else {
                LOG.error("Cannot compare given objects");
                return 0;
            }
        }
    };

    @Override
    protected TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> doInBackground() throws Exception {

        setLabelText("Parsing variants");

        int stepNumber = 0;

        /**
         * Map variants to samples NB: keys in a TreeMap are sorted
         */
        TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> variantToSampleMap = readVariantToSampleMap(inFile);

        LOG.info("Unique variants " + variantToSampleMap.keySet().size());

        List<SimpleFamilyMattersVariant> variants = new ArrayList<SimpleFamilyMattersVariant>(variantToSampleMap.keySet());

        this.setLabelText("Sorting genes");
        Collection<Gene> msGenes = GeneSetController.getInstance().getCurrentGenes();
        List<SimpleFamilyMattersGene> genes = new ArrayList<SimpleFamilyMattersGene>();
        for (Gene g : msGenes) {
            genes.add(new SimpleFamilyMattersGene(g.getChrom(), g.getStart(), g.getCodingEnd(), g.getName()));
        }
        Collections.sort(genes);
        //this.setLabelText("Done sorting genes");

        Comparator startComparator = new VariantGeneComparator(true);
        Comparator endComparator = new VariantGeneComparator(false);

        int zeroIndex = variants.size();

        TreeMap<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>> genesToVariantsMap = new TreeMap<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>>();

        this.setLabelText("Mapping variants to genes");
        for (SimpleFamilyMattersGene g : genes) {
            int s = Collections.binarySearch(variants, g, startComparator);
            int e = Collections.binarySearch(variants, g, endComparator);

            if (s == zeroIndex) {
                s = 0;
            }
            if (e == zeroIndex) {
                e = 0;
            }

            if (s < 0) {
                s = s * -1;
            }
            if (e < 0) {
                e = e * -1;
            }

            if (s > variants.size()) {
                continue;
            }
            if (e > variants.size()) {
                e = variants.size();
            }

            Set<SimpleFamilyMattersVariant> variantsMappingToGene = new HashSet<SimpleFamilyMattersVariant>();

            for (int i = s; i < e; i++) {
                SimpleFamilyMattersVariant v = variants.get(i);
                v.addGene(g);
                variantsMappingToGene.add(v);
            }

            genesToVariantsMap.put(g, variantsMappingToGene);
        }

        for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {
            if (v.pos == 14930) {
                System.out.println("BEFORE EXCLUDE STEP 14930 has " + variantToSampleMap.get(v));
            }
        }

        for (FamilyMattersOptionView.IncludeExcludeStep step : steps) {

            ++stepNumber;

            for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {
                if (v.pos == 14930) {
                    System.out.println("T 14930 has " + variantToSampleMap.get(v));
                }
            }

            LOG.info("Getting gene to sample map");
            Map<SimpleFamilyMattersGene, SimplePatientSet> geneToSampleMap = getGeneToSampleMap(variantToSampleMap);

            for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {
                if (v.pos == 14930) {
                    System.out.println("T-4 14930 has " + variantToSampleMap.get(v));
                }
            }

            Set<SimpleFamilyMattersVariant> allExcludedVariants = new HashSet<SimpleFamilyMattersVariant>();

            int criteriaNumber = 0;

            for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {
                if (v.pos == 14930) {
                    System.out.println("U 14930 has " + variantToSampleMap.get(v));
                }
            }

            for (FamilyMattersOptionView.IncludeExcludeCriteria criterion : step.getCriteria()) {

                ++criteriaNumber;

                LOG.info("Executing criteria #" + criteriaNumber + " of " + step.getCriteria().size() + " of step #" + stepNumber);
                LOG.info(criterion);

                setLabelText("Executing criteria #" + criteriaNumber + " of " + step.getCriteria().size() + " of step #" + stepNumber);


                Set<String> setOfDNAIDs = criterion.getDNAIDs(); //TODO: write method
                //List<String> dnaIDsInCohort = MedSavantClient.CohortManager.getDNAIDsForCohort(LoginController.getInstance().getSessionID(), criterion.getCohort().getId());

                Set<SimpleFamilyMattersVariant> excludedVariantsFromThisStep;
                int frequencyThreshold = getFrequencyThresholdForCriterion(criterion);

                LOG.info("Threshold is " + frequencyThreshold);
                LOG.info("Threshold type is " + criterion.getFrequencyType());

                FamilyMattersOptionView.IncludeExcludeCriteria.AggregationType at = criterion.getAggregationType();
                if (at == FamilyMattersOptionView.IncludeExcludeCriteria.AggregationType.Variant) {

                    LOG.info("Type is variant");
                    excludedVariantsFromThisStep = (Set<SimpleFamilyMattersVariant>) ((Object) flagObjectsForRemovalByCriterion((Map<Object, Set<SimplePatient>>) ((Object) variantToSampleMap), frequencyThreshold, criterion.getFrequencyType(), setOfDNAIDs));
                } else {

                    LOG.info("Type is gene");

                    LOG.info("Size of gene map is " + geneToSampleMap.keySet().size());

                    Set<SimpleFamilyMattersGene> genesToRemove = (Set<SimpleFamilyMattersGene>) ((Object) flagObjectsForRemovalByCriterion((Map<Object, Set<SimplePatient>>) ((Object) geneToSampleMap), frequencyThreshold, criterion.getFrequencyType(), setOfDNAIDs));

                    LOG.info("Excluding " + genesToRemove.size() + " genes");

                    excludedVariantsFromThisStep = new HashSet<SimpleFamilyMattersVariant>();
                    for (SimpleFamilyMattersGene g : genesToRemove) {
                        Set<SimpleFamilyMattersVariant> variantsToExclude = genesToVariantsMap.get(g);
                        excludedVariantsFromThisStep.addAll(variantsToExclude);
                    }
                }

                LOG.info("Excluding " + excludedVariantsFromThisStep.size() + " variants from this step");
                allExcludedVariants.addAll(excludedVariantsFromThisStep);
            }

            for (SimpleFamilyMattersVariant v : allExcludedVariants) {
                variantToSampleMap.remove(v);
            }

            for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {
                if (v.pos == 14930) {
                    System.out.println("AFTER EXCLUDE STEP 14930 has " + variantToSampleMap.get(v));
                }
            }
        }

        InheritanceModel model = inheritanceStep.getInheritanceModel();

        System.out.println("Running inheritance model " + model);

        // adjust for inheritance model
        if (model != InheritanceModel.ANY) {

            Set<String> familyIDs = inheritanceStep.getFamilies();
            List<SimpleFamily> families = getSimpleFamiliesFromIDs(familyIDs);

            if (model == InheritanceModel.AUTOSOMAL_DOMINANT
                    || model == InheritanceModel.AUTOSOMAL_RECESSIVE
                    || model == InheritanceModel.X_LINKED_DOMINANT
                    || model == InheritanceModel.X_LINKED_RECESSIVE
                    || model == InheritanceModel.DE_NOVO) {
                filterForSimpleModels(model, variantToSampleMap, families);

            } else if (model == InheritanceModel.COMPOUND_HETEROZYGOTE) {
                filterForCompoundHeterozygote(variantToSampleMap, genesToVariantsMap, families);
            }

        }
        //TODO do inheritance model : inheritanceStep

        return variantToSampleMap;
    }

    private List<SimpleFamily> getSimpleFamiliesFromIDs(Set<String> familyIDs) throws SQLException, RemoteException {
        List<SimpleFamily> families = new ArrayList<SimpleFamily>();
        for (String familyID : familyIDs) {
            families.add(getSimpleFamilyFromID(familyID));
        }
        return families;
    }

    private SimpleFamily getSimpleFamilyFromID(String familyID) throws SQLException, RemoteException {

        try {
            List<Object[]> results = MedSavantClient.PatientManager.getFamily(LoginController.getInstance().getSessionID(), ProjectController.getInstance().getCurrentProjectID(), familyID);


            SimpleFamily fam = new SimpleFamily();
            for (Object[] o : results) {

                for (Object m : o) {
                    System.out.print(m + "\t");
                }
                System.out.println();

                Integer genderNum = (Integer) o[4];
                Gender gender = Gender.values()[genderNum];

                SimplePerson p = new SimplePerson(
                        (String) o[1], // mom
                        (String) o[2], // dad
                        (String) o[6], // dnaID
                        (Integer) o[5] == 1, // affected
                        gender);
                fam.addPerson(p);
            }

            /*for (SimplePerson p : fam.getFamilyMembers()) {
             if (p.getMomDNAID() != null) {
             SimplePerson mother = fam.getPersonByID(p.getMomDNAID());
             p.setMother(mother);
             }
             if (p.getDadDNAID() != null) {
             SimplePerson father = fam.getPersonByID(p.getDadDNAID());
             p.setMother(father);
             }
             }*/

            return fam;

        } catch (SessionExpiredException ex) {
            MedSavantExceptionHandler.handleSessionExpiredException(ex);
            return null;

        }

    }

    public class SimpleFamily {

        private Map<String, SimplePerson> familyMembers; // dna id to person map

        public SimpleFamily() {
            this.familyMembers = new HashMap<String, SimplePerson>();
        }

        public void addPerson(SimplePerson p) {
            familyMembers.put(p.getDnaID(), p);
        }

        public List<SimplePerson> getFamilyMembers() {
            return new ArrayList<SimplePerson>(familyMembers.values());
        }

        private SimplePerson getPersonByID(String dnaID) {
            return familyMembers.get(dnaID);
        }
    }

    public class SimplePerson {

        //private SimplePerson mother;
        //private SimplePerson father;
        private String momDNAID;
        private String dadDNAID;
        private String dnaID;
        private boolean affected;
        private Gender gender;

        public SimplePerson(String momDNAID, String dadDNAID, String dnaID, boolean affected, Gender gender) {
            this.momDNAID = (momDNAID == null || momDNAID.equals("")) ? null : momDNAID;
            this.dadDNAID = (dadDNAID == null || dadDNAID.equals("")) ? null : dadDNAID;
            this.dnaID = dnaID;
            this.affected = affected;
            this.gender = gender;
        }

        /*public void setMother(SimplePerson mother) {
         this.mother = mother;
         }

         public void setFather(SimplePerson father) {
         this.father = father;
         }*/
        public String getMomDNAID() {
            return momDNAID;
        }

        public String getDadDNAID() {
            return dadDNAID;
        }

        public Gender getGender() {
            return gender;
        }


        /*public SimplePerson getMother() {
         return mother;
         }

         public SimplePerson getFather() {
         return father;
         }*/
        public String getDnaID() {
            return dnaID;
        }

        public boolean isAffected() {
            return affected;
        }

        public boolean areBothParentsLinked() {
            return this.momDNAID != null && this.dadDNAID != null;
        }

        @Override
        public String toString() {
            return "SimplePerson{" + "momDNAID=" + momDNAID + ", dadDNAID=" + dadDNAID + ", dnaID=" + dnaID + '}';
        }

        private boolean atLeastOneParentIsLinked() {
            if (momDNAID != null || dadDNAID != null) {
                return true;
            }
            return false;
        }
    }

    private void writeVariantToSampleMap(Map<SimpleFamilyMattersVariant, Set<String>> map, File outFile) throws IOException {
        CSVWriter w = new CSVWriter(new FileWriter(outFile));

        for (SimpleFamilyMattersVariant v : map.keySet()) {
            String[] arr = new String[]{map.get(v).toString(), v.chr, v.pos + "", v.ref, v.alt, v.type};
            w.writeNext(arr);
        }

        w.close();
    }

    // this is how it's encoded in the db, unknown = 0, male = 1, female = 2
    public static enum Gender {

        UNKNOWN, MALE, FEMALE
    };

    protected class SimplePatient implements Comparable {

        private String dnaID;
        private Zygosity zygosity;
        //private Gender gender;

        public SimplePatient(String dnaID, Zygosity zygosity) {//, Gender gender) {
            this.dnaID = dnaID;
            this.zygosity = zygosity;
            //this.gender = gender;
        }

        public String getDnaID() {
            return dnaID;
        }

        public Zygosity getZygosity() {
            return zygosity;
        }

        @Override
        public String toString() {
            return dnaID + " (" + zygosity.toString() + ")";
        }

        @Override
        public int compareTo(Object t) {
            if (t instanceof SimplePatient) {
                return ((SimplePatient) t).getDnaID().compareTo(dnaID);
            } else {
                return -1;
            }
        }

        /*public Gender getGender() {
         return gender;
         }

         @Override
         public int hashCode() {
         int hash = 3;
         hash = 67 * hash + (this.dnaID != null ? this.dnaID.hashCode() : 0);
         hash = 67 * hash + (this.zygosity != null ? this.zygosity.hashCode() : 0);
         hash = 67 * hash + (this.gender != null ? this.gender.hashCode() : 0);
         return hash;
         }

         @Override
         public boolean equals(Object obj) {
         if (obj == null) {
         return false;
         }
         if (getClass() != obj.getClass()) {
         return false;
         }
         final SimplePatient other = (SimplePatient) obj;
         if ((this.dnaID == null) ? (other.dnaID != null) : !this.dnaID.equals(other.dnaID)) {
         return false;
         }
         if (this.zygosity != other.zygosity) {
         return false;
         }
         if (this.gender != other.gender) {
         return false;
         }
         return true;
         }*/
        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SimplePatient other = (SimplePatient) obj;
            if ((this.dnaID == null) ? (other.dnaID != null) : !this.dnaID.equals(other.dnaID)) {
                return false;
            }
            if (this.zygosity != other.zygosity) {
                return false;
            }
            return true;
        }
    }

    private TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> readVariantToSampleMap(File f) throws FileNotFoundException, IOException {
        TreeMap<SimpleFamilyMattersVariant, SimplePatientSet> map = new TreeMap<SimpleFamilyMattersVariant, SimplePatientSet>();

        CSVReader r = new CSVReader(new FileReader(f), '\t', '\"');

        String[] line = new String[0];

        while ((line = readNext(r)) != null) {
            SimpleFamilyMattersVariant v = variantFromLine(line);

            String sample = line[0];

            Zygosity zygosity = Zygosity.valueOf(line[6]);

            SimplePatient pg = new SimplePatient(sample, zygosity);

            SimplePatientSet samples;
            if (map.containsKey(v)) {
                samples = map.get(v);
            } else {
                samples = new SimplePatientSet();
            }

            samples.add(pg);

            map.put(v, samples);
        }

        return map;
    }

    private String[] readNext(CSVReader recordReader) throws IOException {
        String[] line = recordReader.readNext();
        if (line == null) {
            return null;
        } else {
            line[line.length - 1] = removeNewLinesAndCarriageReturns(line[line.length - 1]);
        }

        return line;
    }

    private String removeNewLinesAndCarriageReturns(String next) {

        next = next.replaceAll("\n", "");
        next = next.replaceAll("\r", "");

        return next;
    }

    private void clear(String[] subsetLines) {
        for (int i = 0; i < subsetLines.length; i++) {
            subsetLines[i] = null;
        }
    }

    // line in format: "8243_0000","chr17","6115","G","C","SNP","Hetero"
    private SimpleFamilyMattersVariant variantFromLine(String[] line) {
        return new SimpleFamilyMattersVariant(line[1], Integer.parseInt(line[2]), line[3], line[4], line[5]);
    }

    private int getFrequencyThresholdForCriterion(FamilyMattersOptionView.IncludeExcludeCriteria criterion) throws SQLException, RemoteException {
        FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType ft = criterion.getFrequencyType(); // all, no, some
        FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyCount fc = criterion.getFequencyCount(); // count, percent
        //Cohort c = criterion.getCohort();
        Set<String> dnaIDs = criterion.getDNAIDs();
        int frequencyThreshold = criterion.getFreqAmount();

        /*List<SimplePatient> patientsInCohort;
         try {
         patientsInCohort = MedSavantClient.CohortManager.getIndividualsInCohort(
         LoginController.getInstance().getSessionID(),
         ProjectController.getInstance().getCurrentProjectID(),
         c.getId());
         } catch (SessionExpiredException ex) {
         MedSavantExceptionHandler.handleSessionExpiredException(ex);
         return 0;
         }*/

        if (ft.equals(FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.ALL)) {
            frequencyThreshold = dnaIDs.size();
        } else if (ft.equals(FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.NO)) {
            frequencyThreshold = 0;
        } else if (fc.equals(FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyCount.Percent)) {
            frequencyThreshold = (int) Math.round(frequencyThreshold / 100.0 * dnaIDs.size());
        }

        return frequencyThreshold;
    }

    /**
     * Flags objects in map for removal if the occurrence in a given set of DNA
     * ids is either above, below, or equal to a frequency threshold
     *
     * @param map A map of object to sample ids relating to the object
     * @param frequencyThreshold The threshold for the cutoff
     * @param t Either ALL, NO, AT_LEAST, AT_MOST
     * @param setOfDNAIDs The set of DNA ids to consider
     * @return The set of objects to be removed
     */
    private Set<Object> flagObjectsForRemovalByCriterion(Map<Object, Set<SimplePatient>> map, int frequencyThreshold, FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType t, Set<String> setOfDNAIDs) {
        Set<Object> removeThese = new HashSet<Object>();
        for (Object o : map.keySet()) {

            // value contains ALL samples having object as key, need to assess
            // only for this cohort
            int numberOfObjectsSamplesInCohort = 0;
            try {
                for (SimplePatient s : map.get(o)) {
                    if (setOfDNAIDs.contains(s.getDnaID())) {
                        numberOfObjectsSamplesInCohort++;
                    }
                }
            } catch (NullPointerException npe) {
                if (map.get(o) == null) {
                    System.out.println("No entry for object in map");
                }
                System.err.println(o);
                throw npe;
            }

            if (t == FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.ALL || t == FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.NO) {
                if (numberOfObjectsSamplesInCohort != frequencyThreshold) {
                    removeThese.add(o);
                }
            } else if (t == FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.AT_LEAST) {
                if (numberOfObjectsSamplesInCohort < frequencyThreshold) {
                    removeThese.add(o);
                }
            } else if (t == FamilyMattersOptionView.IncludeExcludeCriteria.FrequencyType.AT_MOST) {
                if (numberOfObjectsSamplesInCohort > frequencyThreshold) {
                    removeThese.add(o);
                }
            }
        }
        return removeThese;
    }

    private boolean stepIncludesGeneCriterion(FamilyMattersOptionView.IncludeExcludeStep step) {
        for (FamilyMattersOptionView.IncludeExcludeCriteria c : step.getCriteria()) {
            if (c.getAggregationType() == FamilyMattersOptionView.IncludeExcludeCriteria.AggregationType.Gene) {
                return true;
            }
        }
        return false;
    }

    /**
     * Constructs a map from gene to a set of patients (with genotype and other
     * information)
     *
     * @param variantToSampleMap The map from variant to patient set
     * @return The map from gene to patient set
     */
    private Map<SimpleFamilyMattersGene, SimplePatientSet> getGeneToSampleMap(Map<SimpleFamilyMattersVariant, SimplePatientSet> variantToSampleMap) {

        // create an empty map
        Map<SimpleFamilyMattersGene, SimplePatientSet> geneToSampleMap = new HashMap<SimpleFamilyMattersGene, SimplePatientSet>();

        // go through each variant, finding the intersecting gene, and adding to the map
        for (SimpleFamilyMattersVariant v : variantToSampleMap.keySet()) {

            // get genes intersecting this variant
            Set<SimpleFamilyMattersGene> genes = v.getGenes();

            // go through each of these genes, and add the patient set from the variant map
            // to the gene map
            for (SimpleFamilyMattersGene g : genes) {

                // get the samples that have this variant
                SimplePatientSet newSamples = variantToSampleMap.get(v);

                SimplePatientSet setFromMap;

                if (geneToSampleMap.containsKey(g)) {
                    setFromMap = geneToSampleMap.get(g);
                } else {
                    setFromMap = new SimplePatientSet();
                    geneToSampleMap.put(g, setFromMap);
                }

                for (SimplePatient p : newSamples) {
                    setFromMap.add(new SimplePatient(p.dnaID, p.zygosity));
                }
            }
        }

        return geneToSampleMap;
    }

    private Map<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>> mapVariantsToGenesAndViceVersa(Set<SimpleFamilyMattersVariant> variants) throws SQLException, RemoteException {
        Collection<Gene> genes = GeneSetController.getInstance().getCurrentGenes();
        Map<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>> genesToVariantsMap = new HashMap<SimpleFamilyMattersGene, Set<SimpleFamilyMattersVariant>>();

        for (SimpleFamilyMattersVariant v : variants) {
            for (Gene g : genes) {
                if (v.chr.equals(g.getChrom()) && g.getStart() < v.pos && g.getEnd() > v.pos) {
                    SimpleFamilyMattersGene sg = new SimpleFamilyMattersGene(g.getChrom(), g.getStart(), g.getEnd(), g.getName());
                    v.addGene(sg);

                    Set<SimpleFamilyMattersVariant> variantSet;
                    if (genesToVariantsMap.containsKey(sg)) {
                        variantSet = genesToVariantsMap.get(sg);
                    } else {
                        variantSet = new HashSet<SimpleFamilyMattersVariant>();
                    }
                    variantSet.add(v);
                    genesToVariantsMap.put(sg, variantSet);
                }
            }
        }

        return genesToVariantsMap;
    }

    private int binarySearchToVariant(long end, List<SimpleFamilyMattersVariant> variants) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
