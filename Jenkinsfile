@Library('zas-pipelinelibrary') _

mavenPipelineTemplate {
    node='java-25'
    mvnArgs='-B clean verify'
    dockerConfig = [dockerfilePath: '/Dockerfile', imageRoot: 'zas/jweb', imageName: 'laForge']
    email = [recipients: 'matthieu.vinciarelli@zas.admin.ch']
    triggerDevPromotion = [ repositoryName : 'jweb-ocp-promote', versionProperty: 'laForge.image.version' ]
}