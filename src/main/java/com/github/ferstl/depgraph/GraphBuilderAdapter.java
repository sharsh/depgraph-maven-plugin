/*
 * Copyright (c) 2014 by Stefan Ferstl <st.ferstl@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ferstl.depgraph;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import com.github.ferstl.depgraph.DotBuildingVisitor.DoNothingArtifactFilter;
import com.github.ferstl.depgraph.dot.DotBuilder;

/**
 * Adapter for {@link DependencyGraphBuilder} and {@link DependencyTreeBuilder}.
 */
final class GraphBuilderAdapter {

  private DependencyGraphBuilder dependencyGraphBuilder;
  private DependencyTreeBuilder dependencyTreeBuilder;
  private ArtifactRepository artifactRepository;
  private final List<String> targetDependencies;
  
  public GraphBuilderAdapter(DependencyGraphBuilder builder, List<String> targetDependencies) {
    this.dependencyGraphBuilder = builder;
    this.targetDependencies = targetDependencies;
  }
  
  public GraphBuilderAdapter(DependencyTreeBuilder builder, ArtifactRepository artifactRepository, List<String> targetDependencies) {
    this.dependencyTreeBuilder = builder;
    this.artifactRepository = artifactRepository;
    this.targetDependencies = targetDependencies;
  }

  public void buildDependencyGraph(MavenProject project, ArtifactFilter artifactFilter, DotBuilder dotBuilder) {

    if (this.dependencyGraphBuilder != null) {
      createGraph(project, artifactFilter, dotBuilder);
    } else {
      createTree(project, artifactFilter, dotBuilder);
    }
  }

  private void createGraph(MavenProject project, ArtifactFilter artifactFilter, DotBuilder dotBuilder) throws DependencyGraphException {
    org.apache.maven.shared.dependency.graph.DependencyNode root;
    try {
      root = this.dependencyGraphBuilder.buildDependencyGraph(project, artifactFilter);
    } catch (DependencyGraphBuilderException e) {
      throw new DependencyGraphException(e);
    }

    DotBuildingVisitor visitor = new DotBuildingVisitor(dotBuilder, createTargetDependencies(this.targetDependencies));
    root.accept(visitor);
  }

  private void createTree(MavenProject project, ArtifactFilter artifactFilter, DotBuilder dotBuilder) throws DependencyGraphException {
    org.apache.maven.shared.dependency.tree.DependencyNode root;
    try {
      root = this.dependencyTreeBuilder.buildDependencyTree(project, this.artifactRepository, artifactFilter);
    } catch (DependencyTreeBuilderException e) {
      throw new DependencyGraphException(e);
    }

    // Due to MNG-3236, we need to filter the artifacts on our own.
    DotBuildingVisitor visitor = new DotBuildingVisitor(dotBuilder, artifactFilter, createTargetDependencies(this.targetDependencies));
    root.accept(visitor);
  }
  
  private static ArtifactFilter createTargetDependencies(List<String> targetDependencies) {
    if (targetDependencies.isEmpty()) {
      return DoNothingArtifactFilter.INSTANCE;
    } else {
      return new StrictPatternIncludesArtifactFilter(targetDependencies);
    }
  }
}
