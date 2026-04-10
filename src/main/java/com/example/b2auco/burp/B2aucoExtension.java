package com.example.b2auco.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.example.b2auco.export.RawRequestWriter;
import com.example.b2auco.location.OutputDirectoryResolver;
import com.example.b2auco.location.ResolvedOutputDirectory;
import com.example.b2auco.logging.BatchResultFormatter;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.naming.FilenamePolicy;
import com.example.b2auco.workflow.BackgroundBatchDispatcher;
import com.example.b2auco.workflow.SaveRequestsBatchRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class B2aucoExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("b2auco");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        BurpProjectPathProvider projectPathProvider = new BurpProjectPathProvider();
        OutputDirectoryResolver outputDirectoryResolver = new OutputDirectoryResolver();
        ResolvedOutputDirectory resolvedOutputDirectory = outputDirectoryResolver
                .resolveDefaultOutputDirectory(projectPathProvider.findProjectDirectory(api));
        ExportTarget exportTarget = new ExportTarget(resolvedOutputDirectory.outputDirectory());
        FilenamePolicy filenamePolicy = new FilenamePolicy();
        RawRequestWriter rawRequestWriter = new RawRequestWriter();
        SaveRequestsBatchRunner batchRunner = new SaveRequestsBatchRunner(rawRequestWriter);
        BatchResultFormatter batchResultFormatter = new BatchResultFormatter();
        MontoyaPreparedExportMapper mapper = new MontoyaPreparedExportMapper(filenamePolicy);
        BackgroundBatchDispatcher dispatcher = new BackgroundBatchDispatcher(
                executorService,
                batchRunner,
                batchResultFormatter,
                api.logging()
        );
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(exportTarget, mapper::toPreparedExport, dispatcher);

        api.userInterface().registerContextMenuItemsProvider(provider);
        api.extension().registerUnloadingHandler(executorService::shutdown);
    }
}
