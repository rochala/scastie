package scastie.metals

import java.net.URLClassLoader
import java.nio.file.Path
import java.util.ServiceLoader
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._
import scala.meta.internal.metals.Embedded
import scala.meta.internal.metals.MtagsBinaries
import scala.meta.internal.metals.MtagsResolver
import scala.meta.internal.metals.PresentationCompilerClassLoader
import scala.meta.internal.metals.ScalaVersions
import scala.meta.internal.pc.ScalaPresentationCompiler
import scala.meta.pc.PresentationCompiler
import scala.meta.pc.SymbolSearch
import scala.meta.internal.metals.MetalsSymbolSearch
import scala.meta.internal.metals.Docstrings.apply
import scala.meta.internal.mtags.OnDemandSymbolIndex
import scala.meta.internal.metals.StandaloneSymbolSearch
import scala.meta.internal.metals.WorkspaceSymbolProvider
import scala.meta.internal.pc.EmptySymbolSearch
import scala.meta.internal.pc.WorkspaceSymbolSearch
import scala.meta.io.AbsolutePath
import scala.meta.internal.metals.Docstrings
import scala.meta.pc.SymbolSearchVisitor
import java.{util => ju}
import java.net.URI
import org.eclipse.lsp4j.Location
import scala.meta.pc.ParentSymbols
import java.util.Optional
import scala.meta.pc.SymbolDocumentation
import scala.meta.Dialect
import scala.meta.pc.PresentationCompilerConfig
import scala.meta.internal.mtags.GlobalSymbolIndex
import java.nio.file.Files
import scala.meta.internal.metals.BuildTargets
import scala.meta.internal.metals.ExcludedPackagesHandler
import scala.meta.internal.metals.DefinitionProvider
import scala.meta.internal.metals.WorkspaceSymbolQuery
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import scala.meta.internal.metals.ClasspathSearch
import scala.jdk.CollectionConverters._
import meta.internal.mtags.MtagsEnrichments.XtensionAbsolutePath

class ScastieSymbolSearch(docs: Docstrings, classpathSearch: ClasspathSearch) extends SymbolSearch {
    override def search(
        query: String,
        buildTargetIdentifier: String,
        visitor: SymbolSearchVisitor
    ): SymbolSearch.Result = {
      classpathSearch.search(WorkspaceSymbolQuery.exact(query), visitor)
    }

    override def searchMethods(
        query: String,
        buildTargetIdentifier: String,
        visitor: SymbolSearchVisitor
    ): SymbolSearch.Result = {
      classpathSearch.search(WorkspaceSymbolQuery.exact(query), visitor)
    }

    def definition(symbol: String, source: URI): ju.List[Location] = {
      ju.Collections.emptyList()
    }

    def definitionSourceToplevels(
        symbol: String,
        sourceUri: URI
    ): ju.List[String] = {
      ju.Collections.emptyList()
    }

    override def documentation(
        symbol: String,
        parents: ParentSymbols
    ): Optional[SymbolDocumentation] =
      docs.documentation(symbol, parents)
  }
