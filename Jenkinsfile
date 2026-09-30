@Library('zas-pipelinelibrary') _

mavenPipelineTemplate {
    node='java-25'
    mvnArgs='-B clean verify'
    dockerConfig = [dockerfilePath: '/Dockerfile', imageRoot: 'zas/jweb', imageName: 'la-forge-api']
    email = [recipients: 'matthieu.vinciarelli@zas.admin.ch']
    triggerDevPromotion = [ repositoryName : 'jweb-ocp-promote', versionProperty: 'la-forge-api.image.version' ]
}