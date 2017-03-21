package info.novatec.testit.livingdoc.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

/**
 * The task used for the execution of LD specifications
 *
 * @author Nikolay Nachev
 *
 */
class ExecLivingdoc extends JavaExec {

	private String systemUnderDevelopment

	private File specificationsDir

	private File reportsDir

	private Boolean xmlReports = false

	private String runnerClass = "info.novatec.testit.livingdoc.runner.Main"

	private FileCollection executionClasspath

	@Override
	public void exec() {
		executable System.properties.'java.home' + File.separator + "bin" + File.separator + "java"
		main = this.runnerClass
		classpath = executionClasspath
		args = buildArguments()
		super.exec()
	}

	@Input
	File getSpecificationsDir() {
		return specificationsDir
	}

	@Option(option = "specificationDir", description = "The path to the specifications directory", order = 0)
	void setSpecificationsDir(String specificationsDir) {
		this.specificationsDir = new File(specificationsDir)
	}

	@OutputDirectory
	File getReportsDir() {
		return reportsDir
	}

	@Option(option = "reportsDir", description = "Path to a directory to save the execution reports", order = 0)
	void setReportsDir(String reportsDir) {
		this.reportsDir = new File(reportsDir)
	}

	@Input
	String getSystemUnderDevelopment() {
		return systemUnderDevelopment
	}

	@Option(option = "sud", description = "Colon separated string of the fully qualified fixture factory class and the fixture factory args. (e.g.: info.novatec.testit.livingdoc.systemunderdevelopment.DefaultSystemUnderDevelopment:test)", order = 0)
	void setSystemUnderDevelopment(String systemUnderDevelopment) {
		this.systemUnderDevelopment = systemUnderDevelopment
	}

	@Input
	@Optional
	Boolean getXmlReports() {
		return xmlReports
	}

	@Option(option = "xmlReports", description = "Generate reports as XML", order = 0)
	void setXmlReports(Boolean xmlReports) {
		this.xmlReports = xmlReports
	}

	@Input
	@Optional
	String getRunnerClass() {
		return runnerClass
	}

	@Option(option = "runnerClass", description = "The fully qualified class name of the Livingdoc runner class to be executed. (default: info.novatec.testit.livingdoc.runner.Main)", order = 0)
	void setRunnerClass(String runnerClass) {
		this.runnerClass = runnerClass
	}

	@Input
	FileCollection getExecutionClasspath() {
		return executionClasspath
	}

	@Option(option = "execClasspath", description = "The classpath used for the execution of the LD specifications", order = 0)
	void setExecutionClasspath(String execClasspath) {
		this.executionClasspath = this.project.files(execClasspath.split(File.pathSeparator))
	}

	private List<String> buildArguments() {
		def arguments = []
		arguments.addAll(['-f', this.systemUnderDevelopment])
		arguments.addAll(['-s', this.specificationsDir.absolutePath])
		arguments.addAll(['-o', this.reportsDir.absolutePath])

		return arguments

	}

}
