package com.example.b2auco.burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.example.b2auco.export.RawRequestWriter;
import com.example.b2auco.location.OutputDirectoryResolver;
import com.example.b2auco.logging.BatchResultFormatter;
import com.example.b2auco.model.ExportTarget;
import com.example.b2auco.naming.FilenamePolicy;
import com.example.b2auco.settings.EffectiveFolderResolver;
import com.example.b2auco.settings.FolderSettingsController;
import com.example.b2auco.settings.FolderSettingsTab;
import com.example.b2auco.settings.PreferencesFolderSettingsStore;
import com.example.b2auco.workflow.BackgroundBatchDispatcher;
import com.example.b2auco.workflow.SaveRequestsBatchRunner;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class B2aucoExtension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("b2auco");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        OutputDirectoryResolver outputDirectoryResolver = new OutputDirectoryResolver();
        PreferencesFolderSettingsStore folderSettingsStore = new PreferencesFolderSettingsStore(api.persistence().preferences());
        CurrentProjectIdentityProvider currentProjectIdentityProvider = new CurrentProjectIdentityProvider();
        BurpProjectPathProvider burpProjectPathProvider = new BurpProjectPathProvider();
        EffectiveFolderResolver effectiveFolderResolver = new EffectiveFolderResolver(folderSettingsStore, outputDirectoryResolver);
        Supplier<Optional<Path>> currentProjectIdentity = () -> currentProjectIdentityProvider.findCurrentProjectFilePath(api);
        FolderSettingsController folderSettingsController = new FolderSettingsController(
                folderSettingsStore,
                effectiveFolderResolver,
                currentProjectIdentity
        );
        FolderSettingsTab folderSettingsTab = new FolderSettingsTab(folderSettingsController);
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
        SaveRequestsContextMenuProvider provider = new SaveRequestsContextMenuProvider(
                () -> new ExportTarget(effectiveFolderResolver.resolve(
                        currentProjectIdentity.get(),
                        burpProjectPathProvider.findProjectDirectory(api)
                ).folderPath()),
                mapper::toPreparedExport,
                dispatcher
        );

        api.userInterface().registerSuiteTab("b2auco", folderSettingsTab.panel());
        api.userInterface().registerContextMenuItemsProvider(provider);
        api.extension().registerUnloadingHandler(executorService::shutdown);
    }
}
