ifeq ($(OS),Windows_NT)
	GRADLE_EXECUTABLE = gradlew.cmd
else
	GRADLE_EXECUTABLE = ./gradlew
endif

.PHONY: all
all: clean build

.PHONY: build
build:
	${GRADLE_EXECUTABLE} jar

.PHONY: clean
clean:
	${GRADLE_EXECUTABLE} clean
