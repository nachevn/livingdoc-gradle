package info.novatec.testit.livingdoc.tasks

import info.novatec.testit.livingdoc.document.Document
import info.novatec.testit.livingdoc.dsl.RepositorySortFilterDsl
import info.novatec.testit.livingdoc.report.FileReportGenerator
import info.novatec.testit.livingdoc.report.Report
import info.novatec.testit.livingdoc.repository.DocumentRepository
import info.novatec.testit.livingdoc.tasks.conventions.FreezeTaskConvention
import info.novatec.testit.livingdoc.utils.Repository
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

@ParallelizableTask
class FreezeSpecifications extends DefaultTask {

	private String repositoryUrl

	private String repositoryUid

	private File freezeDirectory

	private String repositoryImplementation

	private RepositorySortFilterDsl specificationsFilter

	@TaskAction
	void freezeSpecifications() {
		this.project.convention.plugins.freezeSpecsFilter = new FreezeTaskConvention()

		File specsFreezeDirectory = new File(freezeDirectory, specificationsFilter.path)

		specsFreezeDirectory.deleteDir()
		specsFreezeDirectory.mkdir()

		logger.info("Start freezing specifications from ${repositoryUrl} to directory ${freezeDirectory.path}${File.separator}${specificationsFilter.path} with repositoryUid ${repositoryUid}.")

		Repository repository = new Repository();
		repository.setType(repositoryImplementation);
		repository.setRoot(repositoryUrl);

		this.freeze(repository.getDocumentRepository(), specsFreezeDirectory)

		logger.info("Freezing from specifications completed.")

		File specSourceDir = new File(specsFreezeDirectory, repositoryUid)

		if (!specSourceDir.exists() || this.isDirEmpty(specSourceDir)) {
			logger.info("No specifications found.")
		} else {
			filterSpecifications(specSourceDir, specsFreezeDirectory)
		}

		logger.info("Freezing and sorting specifications completed.")
	}

	private void freeze(DocumentRepository repository, File specsFreezeDirectory) {
		FileReportGenerator generator = new FileReportGenerator(specsFreezeDirectory);
		generator.adjustReportFilesExtensions(true);
		Document doc;
		Report report;

		List<String> specifications = repository.listDocuments(repositoryUid).findAll {
			it.toString() ==~ /${this.specificationsFilter.filter}/
		}

		logger.info("Freeze specifications {} (filter: {})", specifications, this.specificationsFilter.filter)

		specifications.findAll { it.toString() ==~ /${this.project.FREEZE_SPECS_FILTER}/ }.each() { String specification ->
			long startTime = System.currentTimeSeconds()
			doc = repository.loadDocument(specification);
			report = generator.openReport(specification);
			report.generate(doc);
			generator.closeReport(report);
			long stopTime = System.currentTimeSeconds();
			int seconds = (int) (stopTime - startTime) % 60;
			int minutes = ((int) (stopTime - startTime) - seconds) / 60;
			logger.info("Specification ${specification} was successful freezed (time duration: ${minutes}:${seconds})")
		}
	}

	private filterSpecifications(File specSourceDir, File specsFreezeDirectory) {
		project.copy {
			from(specSourceDir) {
				include "${specificationsFilter.filter.replaceAll('.*', '*')}*"
			}
			into(specsFreezeDirectory)
		}
		specSourceDir.deleteDir()
	}

	private boolean isDirEmpty(File dir) {
		return dir.exists() && dir.directory && (dir.list() as List).empty
	}

	@Input
	String getRepositoryUrl() {
		return repositoryUrl
	}

	@Option(option = "url", description = "The url of the confluence repository")
	void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl
	}

	@Input
	String getRepositoryUid() {
		return repositoryUid
	}

	@Option(option = "uid", description = "The UID of the confluence repository")
	void setRepositoryUid(String repositoryUid) {
		this.repositoryUid = repositoryUid
	}

	@Input
	@Optional
	File getFreezeDirectory() {
		return freezeDirectory
	}

	@Option(option = "freezeDirectory", description = "The path to a directory to freeze the specifications")
	void setFreezeDirectory(String freezeDirectory) {
		this.freezeDirectory = new File(freezeDirectory)
	}

	@Input
	String getRepositoryImplementation() {
		return repositoryImplementation
	}

	@Option(option = "implementation", description = "The fully qualified name of the suit resolver class")
	void setRepositoryImplementation(String repositoryImplementation) {
		this.repositoryImplementation = repositoryImplementation
	}

	@Input
	RepositorySortFilterDsl getSpecificationsFilter() {
		return specificationsFilter
	}

	void setSpecificationsFilter(RepositorySortFilterDsl specificationsFilter) {
		this.specificationsFilter = specificationsFilter
	}
}
