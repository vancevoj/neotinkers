package slimeknights.tconstruct.library.tools.stat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.fixture.MaterialStatsFixture;
import slimeknights.tconstruct.fixture.ToolDefinitionFixture;
import slimeknights.tconstruct.library.materials.MaterialRegistryExtension;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook;
import slimeknights.tconstruct.library.tools.definition.module.material.ToolMaterialHook.WeightedStatType;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.stats.ExtraMaterialStats;
import slimeknights.tconstruct.tools.stats.HandleMaterialStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_ALL_STATS;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_EXTRA;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_HANDLE;
import static slimeknights.tconstruct.fixture.MaterialFixture.MATERIAL_WITH_HEAD;

@ExtendWith(MaterialRegistryExtension.class)
public class CompatibleStatTest extends BaseMcTest {
  @BeforeAll
  static void beforeAll() {
    MaterialItemFixture.init();
  }

  record StatTypesWithMaterials(List<WeightedStatType> statTypes, MaterialNBT materials) {
    /** Gets all stats of the given type */
    public <T extends IMaterialStats> List<T> getStats(MaterialStatsId id) {
      return MaterialStatProvider.listOfCompatibleWith(id, materials, statTypes);
    }

    /** Gets head material stats */
    public List<HeadMaterialStats> getHeads() {
      return getStats(HeadMaterialStats.ID);
    }

    /** Gets handle material stats */
    public List<HandleMaterialStats> getHandles() {
      return getStats(HandleMaterialStats.ID);
    }

    /** Gets extra material stats */
    public List<ExtraMaterialStats> getExtras() {
      return getStats(ExtraMaterialStats.ID);
    }
  }

  /**
   * Gets a builder for the given materials list, validating the size is correct
   * @param materials  List of materials
   * @return  Melee harvest tool stats builder
   */
  static StatTypesWithMaterials withMaterials(IMaterial... materials) {
    List<WeightedStatType> statTypes = ToolMaterialHook.stats(ToolDefinitionFixture.getStandardToolDefinition());
    assertThat(materials).overridingErrorMessage("Given materials list is the wrong size").hasSize(statTypes.size());
    return new StatTypesWithMaterials(statTypes, MaterialNBT.of(materials));
  }

  @Test
  void init_onlyHead() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_HEAD, MATERIAL_WITH_HEAD, MATERIAL_WITH_HEAD);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_onlyHandle() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_HANDLE, MATERIAL_WITH_HANDLE, MATERIAL_WITH_HANDLE);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_onlyExtra() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_EXTRA, MATERIAL_WITH_EXTRA, MATERIAL_WITH_EXTRA);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

  @Test
  void init_allCorrectStats() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_HEAD, MATERIAL_WITH_HANDLE, MATERIAL_WITH_EXTRA);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

  @Test
  void init_wrongOrder() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_HANDLE, MATERIAL_WITH_EXTRA, MATERIAL_WITH_HEAD);

    assertThat(builder.getHeads()).containsExactly(HeadMaterialStats.DEFAULT);
    assertThat(builder.getHandles()).containsExactly(HandleMaterialStats.DEFAULT);
    assertThat(builder.getExtras()).containsExactly(ExtraMaterialStats.DEFAULT);
  }

  @Test
  void init_singleMaterialAllStats() {
    StatTypesWithMaterials builder = withMaterials(MATERIAL_WITH_ALL_STATS, MATERIAL_WITH_ALL_STATS, MATERIAL_WITH_ALL_STATS);

    assertThat(builder.getHeads()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HEAD);
    assertThat(builder.getHandles()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_HANDLE);
    assertThat(builder.getExtras()).containsExactly(MaterialStatsFixture.MATERIAL_STATS_EXTRA);
  }

}
