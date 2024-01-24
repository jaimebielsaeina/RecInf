package IR.Practica5;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Muestar la integración dentro de jena de los indices de lucene para permitir 
 * el ranking de resultados
 * Se utiliza una coleccion de datos de la bbc
 * Lo procesa todo en memoria
 */
public class RankingDeResultadosMem {
	public static void main (String args[]) throws Exception{
		
		//definimos la configuración del repositorio indexado
		EntityDefinition entDef = new EntityDefinition("uri", "name", ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/","name"));
		entDef.set("description", DCTerms.description.asNode());
		TextIndexConfig config = new TextIndexConfig(entDef);	   
	    config.setAnalyzer(new EnglishAnalyzer());
	    config.setQueryAnalyzer(new EnglishAnalyzer());
	    config.setMultilingualSupport(true);
	    
	    //definimos el repositorio indexado todo en memoria
	    Dataset ds1 = DatasetFactory.createGeneral() ;
	    Directory dir =  new RAMDirectory();
	    Dataset ds = TextDatasetFactory.createLucene(ds1, dir, config) ;
		
	    // cargamos el fichero deseado y lo almacenamos en el repositorio indexado	  
        RDFDataMgr.read(ds.getDefaultModel(), "bbcColeccion.ttl") ;
              
       //realizamos una consulta al nombre y la descripcion que use filtros para preguntar por musica
        System.out.println("---------------------------------------------");
        System.out.println("Resultados con filtros. No hay ordenación");
        System.out.println("---------------------------------------------");
        
        
        String q ="prefix foaf: <http://xmlns.com/foaf/0.1/> "
        		+ "prefix text: <http://jena.apache.org/text#> "
        		+ "prefix dct:	<http://purl.org/dc/terms/> "
        		+ "Select distinct ?x  where { "
        		+ "{?x dct:description ?y} union {?x foaf:name ?y}. "
        		+ "filter(regex(?y,\"music\",\"i\"))}";
        
        Query query = QueryFactory.create(q) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
          ResultSet results = qexec.execSelect() ;
          for ( ; results.hasNext() ; ) {
            QuerySolution soln = results.nextSolution() ;
            System.out.println(soln);          
          }
        }
        
      //realizamos una consulta al nombre y la descripcion que que usa lucene para obtener los resultados
      //se hace con union para mostrar que no es al forma adecuada de consultar
        System.out.println("---------------------------------------------");
        System.out.println("Resultados con union. La ordenación es erronea ya que no existen ambos score");
        System.out.println("---------------------------------------------");
        
        q ="prefix foaf: <http://xmlns.com/foaf/0.1/> "
        		+ "prefix text: <http://jena.apache.org/text#> "
        		+ "prefix dct:	<http://purl.org/dc/terms/> "
        		+ "Select ?x ?score1 ?score2 ?scoretot where { "
        		+ "{(?x ?score1) text:query (foaf:name 'music' )} union "
        	    + "{(?x ?score2) text:query (dct:description 'music' )}"
        	    + "bind (coalesce(?score1,0)+coalesce(?score2,0) as ?scoretot) "    
        		+ "} ORDER BY DESC(?scoretot)";
        
        query = QueryFactory.create(q) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
          ResultSet results = qexec.execSelect() ;
          for ( ; results.hasNext() ; ) {
            QuerySolution soln = results.nextSolution() ;
            System.out.println(soln);          
          }
        }
        
      //realizamos una consulta al nombre y la descripcion que que usa lucene para obtener los resultados
        //se hace con optional para mostrar que los resultados son mejores
        System.out.println("---------------------------------------------");
        System.out.println("Resultados con optional. Suma de los pesos es de forma adecuada");
        System.out.println("---------------------------------------------");
                  
        q ="prefix foaf: <http://xmlns.com/foaf/0.1/> "
        		+ "prefix text: <http://jena.apache.org/text#> "
        		+ "prefix dct:	<http://purl.org/dc/terms/> "
        		+ "Select ?x ?score1 ?score2 ?scoretot where { " 
        	    + "optional {(?x ?score2) text:query (dct:description 'music' )}. "
        	    + "optional {(?x ?score1) text:query (foaf:name 'music' )}. "
        	    + "bind (coalesce(?score1,0)+coalesce(?score2,0) as ?scoretot) "    
        		+ "} ORDER BY DESC(?scoretot)";
        
       
        query = QueryFactory.create(q) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
          ResultSet results = qexec.execSelect() ;
          for ( ; results.hasNext() ; ) {
            QuerySolution soln = results.nextSolution() ;
            System.out.println(soln);          
          }
        }
        
      //para eliminar duplicados quitamos los score
        System.out.println("---------------------------------------------");
        System.out.println("Resultados finales eliminando los duplicados");
        System.out.println("---------------------------------------------");
                  
        q ="prefix dct:\t<http://purl.org/dc/terms/>\n" +
                "\t\t\tprefix text: <http://jena.apache.org/text#>\n" +
                "\t\t\tselect ?document ?score where {\n" +
                "\t\t\t\t?document dct:language 'spa' .\n" +
                "\t\t\t\t?document dct:type ?t . filter(?t = 'TAZ-TFG' || ?t = 'TAZ-TFM') .\n" +
                "\t\t\t\t?document dct:date ?y . filter(?y >= '2003') .\n" +
                "\t\t\t\toptional { (?document ?sc1) text:query (dct:title 'animacion' )} .\n" +
                "\t\t\t\toptional { (?document ?sc2) text:query (dct:description 'animacion' )} .\n" +
                "\t\t\t\toptional { (?document ?sc3) text:query (dct:subject 'animacion' )} .\n" +
                "\t\t\t\toptional { (?document ?sc4) text:query (dct:title 'vision computador' )} .\n" +
                "\t\t\t\toptional { (?document ?sc5) text:query (dct:description 'vision computador' )} .\n" +
                "\t\t\t\toptional { (?document ?sc6) text:query (dct:subject 'vision computador' )} .\n" +
                "\t\t\t\tbind (coalesce(?sc1,0) + coalesce(?sc2,0) + coalesce(?sc3,0) + coalesce(?sc4,0)\n" +
                "\t\t\t\t\t+ coalesce(?sc5,0) + coalesce(?sc6,0) as ?score) .\n" +
                "\t\t\t\tfilter (?score > 0)\n" +
                "\t\t\t} order by desc (?score)";
        
        query = QueryFactory.create(q) ;
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ds)) {
          ResultSet results = qexec.execSelect() ;
          for ( ; results.hasNext() ; ) {
            QuerySolution soln = results.nextSolution() ;
            System.out.println(soln);          
          }
        }
        
		
	}
}
