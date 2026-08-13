pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Emergency only: package without running tests')
  }

  environment {
    BACKEND_JAR = 'server/tutor-bootstrap/target/tutor-bootstrap-0.1.0-SNAPSHOT.jar'
    BACKEND_IMAGE_REPOSITORY = 'english-tutor-agent-backend'
    DEPLOY_ROOT = '/opt/english-tutor-agent'
    DEPLOY_SCRIPT = '/opt/english-tutor-agent/bin/deploy_backend_container_with_jenkins.sh'
    RELEASE_METADATA_DIR = '.jenkins-release'
  }

  stages {
    stage('Validate Workspace') {
      steps {
        sh '''
          set -euo pipefail
          java -version
          docker version
          docker compose version
          test -f server/mvnw
          chmod +x server/mvnw
          test -f server/Dockerfile
          test -f scripts/deploy/docker-compose.backend.yml
          test -f scripts/deploy/deploy_backend_container_with_jenkins.sh
          test -f scripts/deploy/rollback_backend_container.sh
        '''
      }
    }

    stage('Build Backend') {
      steps {
        sh '''
          set -euo pipefail
          cd server
          if [ "${SKIP_TESTS}" = "true" ]; then
            ./mvnw -pl tutor-bootstrap -am -DskipTests package
          else
            ./mvnw -pl tutor-bootstrap -am clean verify
          fi
        '''
      }
    }

    stage('Build Image') {
      steps {
        sh '''
          set -euo pipefail
          test -f "$BACKEND_JAR"
          rm -rf "$RELEASE_METADATA_DIR"
          mkdir -p "$RELEASE_METADATA_DIR"

          commit_sha="$(git rev-parse --short HEAD)"
          release_id="$(date -u +%Y%m%dT%H%M%SZ)-${commit_sha}"
          backend_image="${BACKEND_IMAGE_REPOSITORY}:${release_id}"

          echo "$release_id" > "$RELEASE_METADATA_DIR/release_id"
          echo "$backend_image" > "$RELEASE_METADATA_DIR/backend_image"

          docker build \
            -f server/Dockerfile \
            --build-arg JAR_FILE="$BACKEND_JAR" \
            -t "$backend_image" \
            .
        '''
      }
    }

    stage('Deploy Backend') {
      steps {
        sh '''
          set -euo pipefail
          release_id="$(cat "$RELEASE_METADATA_DIR/release_id")"
          backend_image="$(cat "$RELEASE_METADATA_DIR/backend_image")"

          sudo "$DEPLOY_SCRIPT" \
            --image "$backend_image" \
            --release-id "$release_id" \
            --deploy-root "$DEPLOY_ROOT" \
            --metadata-dir "$WORKSPACE/$RELEASE_METADATA_DIR"
        '''
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'server/tutor-bootstrap/target/*.jar,.jenkins-release/*', allowEmptyArchive: true
    }
    success {
      sh '''
        if [ -f "$RELEASE_METADATA_DIR/release_id" ]; then
          echo "Deployment succeeded: $(cat "$RELEASE_METADATA_DIR/release_id")"
        fi
      '''
    }
    failure {
      sh '''
        set +e
        echo "Deployment failed. Check Jenkins console output and Docker Compose logs on the VPS."
        if [ -f "$RELEASE_METADATA_DIR/previous_release" ]; then
          previous_release="$(cat "$RELEASE_METADATA_DIR/previous_release")"
          if [ -n "$previous_release" ]; then
            echo "Previous release recorded by deploy script: $previous_release"
          fi
        fi
        if [ -f "$RELEASE_METADATA_DIR/previous_image" ]; then
          previous_image="$(cat "$RELEASE_METADATA_DIR/previous_image")"
          if [ -n "$previous_image" ]; then
            echo "Previous image recorded by deploy script: $previous_image"
          fi
        fi
      '''
    }
  }
}
