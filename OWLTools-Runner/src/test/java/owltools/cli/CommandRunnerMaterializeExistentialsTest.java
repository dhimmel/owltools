package owltools.cli;

import org.junit.Test;

/**
 * Tests for {@link CommandRunner}.
 * 
 * these are somewhat ad-hoc at the moment - output is written to stdout;
 * no attempt made to check results
 * 
 */
public class CommandRunnerMaterializeExistentialsTest extends AbstractCommandRunnerTest {
	

	
	@Test
	public void testMaterialize() throws Exception {
		run("--create-ontology test");
		load("forebrain.obo");
		run("--materialize-existentials -p BFO:0000050");
		run("--add-imports-from-supports -o target/forebrain-partview.owl");
		
		
	}

	


	
	
}
