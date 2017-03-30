/*
 * Copyright 2016, 2017 by HeBIS (www.hebis.de).
 * 
 * This file is part of HeBIS project Gnd4Index.
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
package de.hebis.it.hds.gnd.in;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;

import de.hebis.it.hds.tools.streams.TextBlockSpliterator;

/**
 * Import program for GND authority record files provided by the DNB.<br/>
 * The input files need to bee formated as marc21-XML.<br/>
 * The processed records will be send to an solr index.
 * 
 * @author Uwe Reh (uh), HeBIS-IT
 * @version 2017-03-17 uh First try
 * 
 **/
public class Loader {

   /** The Constant LOG. */
   private static final Logger            LOG          = LogManager.getLogger(Loader.class);
   private static final Predicate<String> startpattern = Pattern.compile(".*<record.*").asPredicate();
   private static final Predicate<String> endpattern   = Pattern.compile(".*</record.*").asPredicate();
   private static SolrClient              server       = null;

   /**
    * Instantiates a new loader.
    *
    * @param baseSolrURL The URL to the repository (solr core)
    */
   public Loader(String baseSolrURL) {
      server = new ConcurrentUpdateSolrClient.Builder(baseSolrURL).withQueueSize(100).withThreadCount(100).build();
      if (server == null) throw new RuntimeException("Can't initialize the solrj client.");
      LOG.debug("SolrWriter is connected to " + baseSolrURL);
   }

   /**
    * Load.
    *
    * @param marcXmlFile the marc xml file
    */
   public void load(URI marcXmlFile) {
      LOG.debug("Starting with " + marcXmlFile.toString());
      Path path2InputFile = Paths.get(marcXmlFile);
      Stream<String> lineStream;
      try {
         lineStream = Files.lines(path2InputFile);
      } catch (IOException e) {
         LOG.fatal("Fehler beim Lesen der Eingabedatei: " + path2InputFile.toString());
         throw new RuntimeException(e);
      }
      // group the lines. TODO find better code
      Stream<List<String>> marcXmlStream = TextBlockSpliterator.toTextBlocks(lineStream, startpattern, endpattern, true);
      // process the data. map and consume
      marcXmlStream.map(new MarcXmlParser(server)).forEach(x -> {if (!x) System.err.println("Fail");});
      LOG.debug("Finished with " + marcXmlFile.toString());
      try {
         server.commit();
      } catch (SolrServerException | IOException e) {
         LOG.error("Failed sending final commit for:" + marcXmlFile.toString() + " to " + server.toString(), e);
         throw new RuntimeException(e);
      }
   }
}