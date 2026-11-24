package com.example.launcherai.network

object NovaNetworkRepository {

    val discoveredEndpoints: List<EndpointInfo> = listOf(
        EndpointInfo(
            category = "Nova RemoteConfig",
            method = "GET",
            url = "https://api.novalauncher.com/remoteconfig/{versionCode}",
            description = "Fetches dynamic feature flags, experiment parameters, and remote UI configuration based on the installed build version code.",
            details = "Interface: j10.a3#a(int versionCode)\nHeaders: User-Agent: com.teslacoilsw.launcher/{version}\nResponse: JSON object containing feature toggles and remote limits."
        ),
        EndpointInfo(
            category = "Nova Update Service",
            method = "GET",
            url = "https://api.novalauncher.com/latestVersion?packageName={pkg}&changelog_from_versionCode={code}&betaType={beta}",
            description = "Checks for official app updates, retrieves beta channel advisories, and pulls version-to-version release notes.",
            details = "Interface: j10.a3#b\nParameters: changelog_from_versionCode, betaType, packageName\nCaller: ChangeLogDialog.java / MyPackageReplacedReceiver.java"
        ),
        EndpointInfo(
            category = "Nova Telemetry & Logs",
            method = "POST",
            url = "https://api.novalauncher.com/stats/rawDataLog/{userId}/{timestamp}",
            description = "Submits raw diagnostic payloads, system crash contexts, and launcher usage metrics as compressed byte streams.",
            details = "Caller: SubmitRawDataLogWork.java (CoroutineWorker) / r10.b.java\nHeaders: User-Agent: com.teslacoilsw.launcher/89500\nPayload: Binary compressed event batches"
        ),
        EndpointInfo(
            category = "Nova Prime Licensing",
            method = "POST",
            url = "https://api.novalauncher.com/verifyLicense",
            description = "Validates Nova Launcher Prime purchase tokens, activation keys, and direct licensing signatures.",
            details = "Caller: DirectLicensingEnterCode.java\nHeaders: Authorization token & device fingerprint\nResponse: License status, expiry, and entitlement flags"
        ),
        EndpointInfo(
            category = "Nova Geolocation",
            method = "GET",
            url = "https://api.novalauncher.com/location",
            description = "Resolves country, region, and IP-based locale parameters for local search and integration recommendations.",
            details = "Caller: b10.d0.java / d30.d.java\nUse: Feature availability filtering and regional search customization."
        ),
        EndpointInfo(
            category = "Nova Experimentation",
            method = "GET",
            url = "https://api.novalauncher.com/x",
            description = "A/B testing and dynamic experimentation parameters for new UI flows, animations, and onboarding journeys.",
            details = "Caller: v30.h.java\nResponse: Variant allocation and experiment group IDs"
        ),
        EndpointInfo(
            category = "Ad Mediation Config",
            method = "GET",
            url = "https://api.novalauncher.com/mediationconfig.json",
            description = "Distributes dynamic ad network mediation waterfalls, provider priorities, and timeout thresholds.",
            details = "Caller: wx.c.java\nFormats: JSON array of ad networks, placements, and unit IDs"
        ),
        EndpointInfo(
            category = "Branch Ingestion Telemetry",
            method = "POST",
            url = "https://stage-nova-analytics-ingestion.eks-stage-magpie-usw1.branch.io/v1/ingest",
            description = "Branch Metrics analytics pipeline ingesting user session events, retention cohorts, and deep-link conversions.",
            details = "Resource: res/values/strings.xml (novadailystats_endpoint)\nPayload: Session tracking events, daily active stats, screen impressions."
        ),
        EndpointInfo(
            category = "Instabridge AI Platform",
            method = "POST / SSE",
            url = "https://api.instabridge.com/api3/",
            description = "Instabridge AI conversational backend powering Nova AI streaming text, reasoning chains, and agent tool execution.",
            details = "Caller: com.instabridge.ai.client.AiClient.java / AiChatViewV2.java\nFeatures: Streaming tokens, reasoning blocks, tool callbacks, and human-in-the-loop"
        ),
        EndpointInfo(
            category = "Instabridge Staging AI",
            method = "POST / SSE",
            url = "https://api.instabridge.dev/api3/",
            description = "Development and internal test environment for upcoming Instabridge AI agent and reasoning capabilities.",
            details = "Source: MobileSdkConfigKt.java (debug mode toggle)"
        ),
        EndpointInfo(
            category = "Instabridge Connectivity",
            method = "REST API",
            url = "https://esim.instabridge.com/",
            description = "eSIM provisioning and connectivity profile management SDK bundled with the Instabridge mobile services.",
            details = "Source: MobileSdkConfigKt.java / com.instabridge.mobilesdk"
        ),
        EndpointInfo(
            category = "Sesame Search Relay",
            method = "IPC / Intent Bridge",
            url = "ninja.sesame.lib.bridge.v1.access.RelayActivity",
            description = "Deep on-device indexing and cross-app shortcut search integration with Sesame Shortcuts.",
            details = "Manifest: RelayActivity.java & IntegrationActivity.java\nMechanism: Android Binder IPC for instant contact, app, and web queries."
        )
    )
}
