package info.novatec.testit.livingdoc.tasks

import info.novatec.testit.livingdoc.conventions.FreezeTaskConvention
import info.novatec.testit.livingdoc.document.Document
import info.novatec.testit.livingdoc.dsl.RepositoryFixtureFilterDsl
import info.novatec.testit.livingdoc.report.FileReportGenerator
import info.novatec.testit.livingdoc.report.Report
import info.novatec.testit.livingdoc.repository.DocumentRepository
import info.novatec.testit.livingdoc.server.LivingDocServerException
import info.novatec.testit.livingdoc.server.domain.RepositoryType
import info.novatec.testit.livingdoc.server.domain.Repository
import info.novatec.testit.livingdoc.server.rest.LivingDocRestClient


import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

import java.nio.charset.StandardCharsets

@ParallelizableTask
class FreezeTask extends DefaultTask {

    @Input
    String repositoryUrl

    @Input
    String repositoryUid

    @Input
    File freezeDirectory

    @Input
    String repositoryImplementation

    @Input
    RepositoryFixtureFilterDsl specificationsFilter

    @TaskAction
    void freezeSpecifications() {
        assert repositoryUrl != null, 'Error: The repositoryUrl cannot be null'
        assert repositoryUid != null, 'Error: The repositoryUid cannot be null'
        assert specificationsFilter != null, 'Error: The specificationFilter cannot be null'
        assert repositoryImplementation != null, 'Error: The repositoryImplementation cannot be null'


        this.project.convention.plugins.freezeSpecsFilter = new FreezeTaskConvention()

        File specsFreezeDirectory = new File(freezeDirectory, specificationsFilter.path)

        specsFreezeDirectory.deleteDir()
        specsFreezeDirectory.mkdir()

        logger.info("Start freezing specifications from ${repositoryUrl} to directory ${freezeDirectory.path}${File.separator}${specificationsFilter.path} with repositoryUid ${repositoryUid}.")

        LivingDocRestClient livingDocRestClient = new LivingDocRestClient(repositoryUrl, "nachevn", "geheim123");
        println "Test connection"
        println livingDocRestClient.testConnection(null, null)

        Repository repository = convertToRepository("Confluence-LIVINGDOCDEMO", "Test", repositoryUrl, repositoryImplementation);

        DocumentRepository documentRepository = repository.asDocumentRepository(this.getClass().getClassLoader(), "nachevn", "gemeihm123")

        logger.lifecycle("Here: {}<-", documentRepository.listDocuments("Confluence-LIVINGDOCDEMO"))

        info.novatec.testit.livingdoc.utils.Repository repositoryOld = new info.novatec.testit.livingdoc.utils.Repository();
        repositoryOld.setType(repositoryImplementation);
        repositoryOld.setRoot(repositoryUrl);

        this.freeze(repositoryOld.getDocumentRepository(), specsFreezeDirectory)

        logger.info("Freezing from specifications completed.")

        File specSourceDir = new File(specsFreezeDirectory, repositoryUid)

        if (!specSourceDir.exists() || this.dirEmpty(specSourceDir)) {
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

        List<String> specifications = repository.listDocuments(repositoryUid).findAll { it.toString() ==~ /${this.specificationsFilter.filter}/ }

        logger.info("Freeze specifications {} (filter: {})", specifications, this.specificationsFilter.filter)

        specifications.findAll{ it.toString() ==~ /${this.project.FREEZE_SPECS_FILTER}/ }.each() { String specification ->
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

    private boolean dirEmpty(File dir) {
        return dir.exists() && dir.directory && (dir.list() as List).empty
    }

    private Repository convertToRepository(String repositoryUID, String repositoryName, String repositoryURL, String implementation) {

        Repository repository = Repository.newInstance(repositoryUID);
        repository.setName(repositoryName);
        repository.setBaseTestUrl(repositoryURL);
        logger.lifecycle("Set repositoryUID {} for repositoryName {} and repositoryURL {}", repositoryUID, repositoryName, repositoryURL)
        RepositoryType repositoryType = new RepositoryType();
        repositoryType.setClassName(implementation);
        logger.lifecycle("Set repository implementation {}", implementation)
        repository.setType(repositoryType);
        return repository;
    }
}
