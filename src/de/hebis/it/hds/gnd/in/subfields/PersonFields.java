/*
 * Copyright 2016, 2017 by HeBIS (www.hebis.de).
 * 
 * This file is part of HeBIS HdsToolkit.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the code.  If not, see http://www.gnu.org/licenses/agpl>.
 */
package de.hebis.it.hds.gnd.in.subfields;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Methods for persons
 * <dl>
 * <dt>Referenced definitions:</dt>
 * <dd>Basics: <a href="https://www.loc.gov/marc/authority/">LOC: MARC 21 Format for Authority Data</a></dd>
 * <dd>Extentions: "Normdaten (GND)" at <a href="http://www.dnb.de/DE/Standardisierung/Formate/MARC21/marc21_node.html">DNB: MARC 21</a></dd>
 * </dl>
 *
 * @author Uwe Reh (uh), HeBIS-IT
 * @version 04.04.2017 uh initial
 */
public class PersonFields {
   private final static Logger LOG = LogManager.getLogger(PersonFields.class);

   /**
    * Personal name &lt;datafield tag="100"&gt;.<br>
    * Subfields will be stored in the form "$a $b &lt;$c&gt;. (schema:prefered)<br>
    * 
    * @param dataField The content of the data field
    */
   public static void headingPersonalName(DataField dataField) {
      if (LOG.isTraceEnabled()) LOG.trace(dataField.getRecordId() + ": in method");
      storePreferred(dataField);
   }

   /**
    * Alternative names, pseudonyms and related persons &lt;datafield tag="[45]00"&gt;.<br>
    * Subfields will be stored in the form "$a $b &lt;$c&gt;. (schema:synonyms)<br>
    * 
    * @param dataField The content of the data field
    */
   public static void tracingPersonalName(DataField dataField) {
      if (LOG.isTraceEnabled()) LOG.trace(dataField.getRecordId() + ": in method");
      List<String> relations = dataField.get("4");
      if (relations != null) {
         for (String relType : relations) {
            switch (relType) {
               case "nawi":
               case "pseu":
                  storeSynonym(dataField);
                  break;
               default:
                  storeRelated(dataField);
            }
         }
      }
      else {
         storeSynonym(dataField);
      }
   }


   /**
    * Alternative names in other systems &lt;datafield tag="700"&gt;.<br>
    * see: {@link GenericFields#linkingEntry(DataField, String)} Optional trailing informations starting with "%DE" are be removed. "ABC%DE3..." will result in "ABC"
    * 
    * @param dataField The content of the data field
    */
   public static void linkingEntryPersonalName(DataField dataField) {
      if (LOG.isTraceEnabled()) LOG.trace(dataField.getRecordId() + ": in method");
      GenericFields.linkingEntry(dataField, "%DE.*");
   }

   private static void storePreferred(DataField dataField) {
      dataField.storeUnique("preferred", buildFormatedName(dataField));   }
   
   private static void storeSynonym(DataField dataField) {
      dataField.storeMultiValued("synonyms", buildFormatedName(dataField));
   }
   
   private static void storeRelated(DataField dataField) {
      GenericFields.related(dataField);   }
   
   private static String buildFormatedName(DataField dataField) {
      // name
      String name = dataField.getFirstValue("a");
      if (name == null) {
         LOG.info(dataField.getRecordId() + ": No $a. in field " + dataField.getFirstValue("tag"));
         return null;
      }
      StringBuilder fullName = new StringBuilder(name);
      // nummeration
      String numeration = dataField.getFirstValue("b");
      if (numeration != null) {
         fullName.append(' ');
         fullName.append(numeration);
      }
      // title(s)
      List<String> titles = dataField.get("c");
      if (titles != null) {
         for (Object title : titles) {
            fullName.append(" <");
            fullName.append((String) title);
            fullName.append('>');
         }
      }
      return fullName.toString();
   }

}
