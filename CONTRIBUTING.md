# Contributing to FreeLibrary

Thank you for considering a contribution. FreeLibrary is built to stay free, private,
and accessible — contributions should align with that mission.

## Getting started

1. Fork the repository and clone your fork locally.
2. Create a branch from `main` for your change: `git checkout -b feature/short-description`
   or `fix/short-description`.
3. Make your changes, following the code style enforced by ktlint (run `./gradlew ktlintCheck`
   before committing — instructions on this will expand once the Android project is in place).
4. Commit using [Conventional Commits](https://www.conventionalcommits.org/) format, e.g.
   `feat: add zoom control to reader screen` or `fix: correct download progress bar state`.
5. Push your branch and open a pull request against `main`.

## One-time setup

After cloning, enable the repository's git hooks (a pre-commit check that runs ktlint
automatically) by running:

git config core.hooksPath .githooks

This is a local git setting and isn't stored in the repository itself, so each clone needs
to run it once.

## Branching model

This project uses a trunk-based workflow: `main` is always kept in a working state, and all
changes land via short-lived feature/fix branches merged through pull requests.

## Code of conduct

Be respectful and constructive. This project serves people in difficult circumstances;
contributions should reflect that seriousness of purpose.

## Reporting issues

Please use the issue templates provided when filing a bug report or feature request.
