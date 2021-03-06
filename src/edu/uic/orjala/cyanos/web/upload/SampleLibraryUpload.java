/**
 * 
 */
package edu.uic.orjala.cyanos.web.upload;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ListIterator;

import edu.uic.orjala.cyanos.DataException;
import edu.uic.orjala.cyanos.Material;
import edu.uic.orjala.cyanos.Sample;
import edu.uic.orjala.cyanos.SampleAccount;
import edu.uic.orjala.cyanos.sql.SQLData;
import edu.uic.orjala.cyanos.sql.SQLMaterial;
import edu.uic.orjala.cyanos.sql.SQLSample;
import edu.uic.orjala.cyanos.web.BaseForm;
import edu.uic.orjala.cyanos.web.SheetValue;
import edu.uic.orjala.cyanos.web.html.HtmlList;

/**
 * @author George Chlipala
 *
 */
public class SampleLibraryUpload extends UploadJob {

	public static final String TITLE = "Sample Library Data";
	
	public static final String PROTOCOL = "sample library upload";

	// Template keys
	public static final String SOURCE_ID = "sourceID";
	public static final String LOAD_DATE = "loadDate";
	public static final String LOAD_AMT_UNIT = "loadAmtUnit";
	public static final String LOAD_AMOUNT = "loadAmt";
	public static final String LOAD_COL = "loadCol";
	public static final String LOAD_CONC = "loadConc";
	public static final String STATIC_CONC = "staticConc";
	public static final String DEST_COLLECTION = "destCollection";
	public static final String DEST_LOCATION = "destLocation";
	public static final String DEST_LABEL = "destLabel";
	public static final String DEST_NOTES = "destNotes";
	public static final String PROJECT_COL = "projectCol";
	public static final String STATIC_PROJECT = "staticProject";
	public static final String STATIC_COLLECTION = "staticCollection";

	public static final String[] templateKeys = { SOURCE_ID,
		LOAD_DATE, LOAD_AMT_UNIT, LOAD_AMOUNT, LOAD_COL, LOAD_CONC, STATIC_CONC,
		DEST_COLLECTION, DEST_LOCATION, DEST_LABEL, DEST_NOTES, PROJECT_COL, STATIC_PROJECT, STATIC_COLLECTION};

	/**
	 * Create a new SampleLibraryUpload.
	 */
	public SampleLibraryUpload(SQLData data) {
		super(data);
		this.type = TITLE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if ( this.working ) return;
		this.done = 0;
		this.todos = this.rowList.size();
		this.working = true;
		// Setup the row iterator.
		ListIterator<Integer> rowIter = this.rowList.listIterator();
		HtmlList resultList = new HtmlList();
		resultList.unordered();
		try {
//			int sourceLibCol = -1, sourceLocCol = -1, 
			int sourceIDCol = -1;

			int loadDateCol = Integer.parseInt(template.get(LOAD_DATE));
			int loadAmt = Integer.parseInt(template.get(LOAD_AMOUNT));
			String loadUnit = template.get(LOAD_AMT_UNIT);
			int loadConcCol = Integer.parseInt(template.get(LOAD_CONC));
			BigDecimal staticConc = BigDecimal.ZERO;
			if ( ! template.get(STATIC_CONC).equals("") ) {
				staticConc = BaseForm.parseAmount(template.get(STATIC_CONC), "mg/ml");
			}
			boolean wksConc = (loadConcCol > 0);

			int destLibCol = Integer.parseInt(template.get(DEST_COLLECTION));
			String staticCol = template.get(STATIC_COLLECTION);
			
			boolean useCollectionCol = ( destLibCol > 0 );
			
			int destLocCol = Integer.parseInt(template.get(DEST_LOCATION));
			int destLabelCol = Integer.parseInt(template.get(DEST_LABEL));
			int destNotesCol = Integer.parseInt(template.get(DEST_NOTES));
			int projectCol = Integer.parseInt(template.get(PROJECT_COL));
			String staticProject = template.get(PROJECT_COL);
			boolean useProjectCol = ( projectCol > -1 );

			sourceIDCol = Integer.parseInt(template.get(SOURCE_ID)); 

			Date myDate = new Date();
			String bulkLoadNote = String.format("\nCreated via bulk load by user: %s\n %s",  
					this.myData.getUser().getUserID(), myDate.toString());

			while (rowIter.hasNext() && this.working ) {
				Integer row = (Integer)rowIter.next();
				if ( this.worksheet.gotoRow(row.intValue()) ) {
					HtmlList currResults = new HtmlList();
					currResults.unordered();
					savepoint = myData.setSavepoint();
					try {
						Material source = SQLMaterial.load(myData, this.worksheet.getStringValue(sourceIDCol));
						if (source != null && source.first()) {
							SheetValue thisDate = this.worksheet.getValue(loadDateCol);
							Sample dest;
							if ( useProjectCol ) dest = SQLSample.createInProject(this.myData, source.getID(), this.worksheet.getStringValue(projectCol));
							else dest = SQLSample.createInProject(this.myData, source.getID(), staticProject);
							dest.setManualRefresh();
							if ( thisDate.isDate() )
								dest.setDate(thisDate.getDate());
							else 
								dest.setDate(thisDate.toString());
							BigDecimal conc = (wksConc ? BaseForm.parseAmount(this.worksheet.getStringValue(loadConcCol), "mg/mL") : staticConc );
							dest.setConcentration(conc);
							if ( useCollectionCol ) { dest.setCollectionID(this.worksheet.getStringValue(destLibCol)); }
							else { dest.setCollectionID(staticCol); }
							if (destLocCol > -1) { dest.setLocation(this.worksheet.getStringValue(destLocCol)); }
							if (destLabelCol > -1) { dest.setName(this.worksheet.getStringValue(destLabelCol)); }
							if (destNotesCol > -1)  { dest.setNotes(bulkLoadNote + this.worksheet.getStringValue(destNotesCol)); }
							else { dest.setNotes(bulkLoadNote); }
							dest.setBaseUnit(loadUnit);
							dest.refresh();
							dest.setAutoRefresh();
							SampleAccount destAcct = dest.getAccount();
							if ( destAcct.addTransaction() ) {
								destAcct.depositAmount(this.worksheet.getStringValue(loadAmt), loadUnit);
								if ( thisDate.isDate() )
									destAcct.setDate(thisDate.getDate());
								else
									destAcct.setDate(thisDate.toString());
								destAcct.setNotes("Initial Amount" + bulkLoadNote);
								destAcct.updateTransaction();
								myData.commit();
								currResults.addItem(SUCCESS_TAG + "Amount deposited into destination.");
//								srcAcct.setDate(thisDate);
//								srcAcct.setNotes("To sample." + bulkLoadNote);
//								currResults.addItem(SUCCESS_TAG + "Amount withdrawn from source.");
//								srcAcct.updateTransaction();
							} else {
								currResults.addItem(ERROR_TAG + "Could not create a transaction for the destination sample.");
								myData.rollback(savepoint);
							}
						} else {
							currResults.addItem(ERROR_TAG + "Could not load source information.");
						}
					} catch (DataException e) {
						myData.rollback(savepoint);
						currResults.addItem(ERROR_TAG + e.getMessage());
						e.printStackTrace();
					} finally {
						myData.releaseSavepoint(savepoint);
					}
					resultList.addItem(String.format("Row #:%d %s",row + 1, currResults.toString()));
				}
				this.done++;
			}

		} catch (Exception e) {
			this.messages.append("<P ALIGN='CENTER'><B><FONT COLOR='red'>ERROR:</FONT>" + e.getMessage() + "</B></P>");
			e.printStackTrace();
			this.working = false;
		}
		this.messages.append(resultList.toString());
		this.finishJob();
	}

	/*
	public String templateForm() {
		Table smallTable;
		TableCell myCell;
		List<String> headerList = this.getHeaderList(template.containsKey(HEADER));

		if ( template.containsKey(HEADER) ) {
			myCell = new TableCell("<INPUT TYPE='CHECKBOX' NAME='header' VALUE='true' onClick='this.form.submit()' CHECKED /> Spreadsheet has a header row.<BR>");					
		} else {
			myCell = new TableCell("<INPUT TYPE='CHECKBOX' NAME='header' VALUE='true' onClick='this.form.submit()' /> Spreadsheet has a header row.<BR>");
		}

		myCell.setAttribute("COLSPAN", "2");
		myCell.setAttribute("ALIGN", "CENTER");
		TableRow fullRow = new TableRow(myCell);

		// Create the header row and build the column popup object.
		Popup ssColPop = new Popup();
		Popup optionalPop = new Popup();
		Popup useOtherPop = new Popup();
		optionalPop.addItemWithLabel("-1", "SKIP ITEM");
		useOtherPop.addItemWithLabel("-1", "Use Value ->");
		for ( int i = 0; i < headerList.size(); i++ ) {
			String index = String.valueOf(i);
			String value = (String)headerList.get(i);
			ssColPop.addItemWithLabel(index, value);
			optionalPop.addItemWithLabel(index, value);
			useOtherPop.addItemWithLabel(index, value);
		}


/*
		if ( ! this.template.containsKey(INFO_TYPE) ) this.template.put(INFO_TYPE, "date");
		String infoType = template.get(INFO_TYPE);

		Popup infoTypePop = new Popup();
		infoTypePop.addItemWithLabel("location", "Sample collection and location");
		infoTypePop.addItemWithLabel("id", "Sample ID");
		infoTypePop.setName(INFO_TYPE);
		infoTypePop.setDefault(infoType);
		infoTypePop.setAttribute("onChange", "this.form.submit();");
	
		myCell = new TableCell("Find source sample by:" + infoTypePop.toString());
		myCell.setAttribute("COLSPAN", "2");
		myCell.setAttribute("ALIGN", "CENTER");
		fullRow.addItem(myCell);

		String[] dateFields = {SOURCE_CULTURE_ID, SOURCE_DATE};
		String[] locationFields = {SOURCE_COLLECTION, SOURCE_LOCATION};
		String[] idFields = {SOURCE_ID};
*/
/*
 		TableRow myRow = new TableRow();
		myRow.addItem("<TH COLSPAN=2 ALIGN='CENTER'>Source Sample Information</TH>");
		StringBuffer hideRow = new StringBuffer("<TD>");
*/
		/*
		if ( infoType.equals("date") ) {
			myRow.addItem(this.simpleTemplateRow("Strain ID:", SOURCE_CULTURE_ID, ssColPop));
			myRow.addItem(this.simpleTemplateRow("Sample Date:", SOURCE_DATE, ssColPop));
			hideRow.append(this.hideTemplateValues(locationFields));
			hideRow.append(this.hideTemplateValues(idFields));
		} else if ( infoType.equals("location") ) {
			myRow.addItem(this.simpleTemplateRow("Collection ID:", SOURCE_COLLECTION, ssColPop));
			myRow.addItem(this.simpleTemplateRow("Sample Location:",SOURCE_LOCATION, ssColPop));
			hideRow.append(this.hideTemplateValues(dateFields));
			hideRow.append(this.hideTemplateValues(idFields));
		} else if ( infoType.equals("id") ) {
		*/
/*			myRow.addItem(this.simpleTemplateRow("Sample ID:", SOURCE_ID, ssColPop));
//			hideRow.append(this.hideTemplateValues(locationFields));
//			hideRow.append(this.hideTemplateValues(dateFields));
//		}

		hideRow.append("</TD>");
		myRow.addItem(hideRow.toString());
		smallTable = new Table(myRow);
		TableCell fullCell = new TableCell(smallTable);
		fullCell.setAttribute("WIDTH", "50%");
		myRow = new TableRow();

		myRow.addItem("<TH COLSPAN=4 ALIGN='CENTER'>Destination Information</TD>");

		myRow.addItem(this.simpleTemplateRow("Date:", LOAD_DATE, ssColPop));
		myRow.addItem(this.simpleTemplateRow("Collection ID:", DEST_COLLECTION, ssColPop));
		myRow.addItem(this.templateRowWithStatic("Concentration (mg/ml):", LOAD_CONC, STATIC_CONC, useOtherPop));
		myRow.addItem(this.simpleTemplateRow("Location:", DEST_LOCATION, optionalPop));
		myRow.addItem(this.templateRowWithUnit("Amount:", LOAD_AMOUNT, LOAD_AMT_UNIT, ssColPop, "ul"));
		myRow.addItem(this.simpleTemplateRow("Label:", DEST_LABEL, optionalPop));
		myRow.addItem(this.simpleTemplateRow("Notes:", DEST_NOTES, optionalPop));
		myRow.addItem(this.projectTemplateRow("Project Code:", PROJECT_COL, useOtherPop, STATIC_PROJECT));

		smallTable = new Table(myRow);
		fullCell.addItem(smallTable);
		myRow = new TableRow();
		fullRow.addItem(fullCell);

		Table formTable = new Table(fullRow);
		formTable.setAttribute("WIDTH", "85%");

		return formTable.toString();
	}
*/
	
	public String[] getTemplateKeys() {
		return templateKeys;
	}
}
