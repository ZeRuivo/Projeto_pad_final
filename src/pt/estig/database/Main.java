package pt.estig.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    // Declaração do objeto de conexão e nome da base de dados
    private static DBConnection db;
    private static String dbName = "gerenciamento_gym";

    public static void main(String[] args) {
        // Inicializa o Scanner para ler dados do teclado
        Scanner scanner = new Scanner(System.in);
        int opcao = -1; // Variável de controlo do menu

        // Ciclo while: O menu repete-se até o utilizador escolher 0 (Sair)
        while (opcao != 0) {
            // Desenho do Menu (Interface de Texto) [cite: 68]
            System.out.println("\n===========================");
            System.out.println("   GYM MANAGEMENT SYSTEM   ");
            System.out.println(" ===========================");
            System.out.println("1. Listar Utilizadores");
            System.out.println("2. Listar Exercícios");
            System.out.println("3. Ver Relatório de Treinos");
            System.out.println("4. Adicionar Novo Utilizador");
            System.out.println("5. Editar Utilizador");
            System.out.println("6. Eliminar Utilizador");
            System.out.println("7. Pesquisar Utilizador por Nome");
            System.out.println("8. Criar Novo Treino");
            System.out.println("9. Adicionar Exercício ao Treino");
            System.out.println("0. Sair");
            System.out.print(  "   Escolha uma opção: ");

            // Leitura da opção numérica
            opcao = scanner.nextInt();
            scanner.nextLine(); // IMPORTANTE: Limpa o buffer ("enter") pendente após ler o número

            // Switch para encaminhar para o método correto baseado na escolha
            switch (opcao) {
                case 1: listarUtilizadores(); break;
                case 2: listarExercicios(); break;
                case 3: relatorioTreinos(); break;
                case 4: inserirUtilizador(scanner); break;
                case 5: editarUtilizadores(scanner); break;
                case 6: eliminarUtilizador(scanner); break;
                case 7: pesquisarUtilizador(scanner); break;
                case 8: criarTreino(scanner); break;
                case 9: adicionarItemTreino(scanner); break;
                case 0: System.out.println("A sair..."); break;
                default: System.out.println("Opção inválida!");
            }
        }
        scanner.close(); // Fecha o scanner ao terminar o programa
    }

    // --- LISTAGEM (READ) ---
    public static void listarUtilizadores() {
        db = new DBConnection(dbName);
        if (db.connect()) { // Abre conexão
            String sql = "SELECT * FROM utilizadores"; // Query simples
            // Try-with-resources: garante que o PreparedStatement fecha automaticamente
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery(); // Executa a query e guarda os resultados em 'rs'
                System.out.println("\n--- LISTA DE UTILIZADORES ---");
                // Percorre linha a linha os resultados da BD
                while (rs.next()) {
                    // Imprime formatado (d=inteiro, s=string)
                    System.out.printf("ID: %d | Nome: %s | Email: %s\n",
                            rs.getInt("id_utilizador"), rs.getString("nome"), rs.getString("email"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            db.close(); // Fecha conexão
        }
    }

    // --- EDIÇÃO (UPDATE) ---
    public static void editarUtilizadores(Scanner scanner) {
        System.out.print("ID do Utilizador a editar: ");
        int id = scanner.nextInt();
        scanner.nextLine(); // Limpar buffer
        System.out.print("Novo Nome: ");
        String nome = scanner.nextLine();
        System.out.print("Novo Email: ");
        String email = scanner.nextLine();

        db = new DBConnection(dbName);
        if (db.connect()) {
            // Query SQL com parâmetros (?) para evitar SQL Injection
            String sql = "UPDATE utilizadores SET nome = ?, email = ? WHERE id_utilizador = ?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                // Substitui os pontos de interrogação pelos valores das variáveis
                ps.setString(1, nome);
                ps.setString(2, email);
                ps.setInt(3, id);

                // executeUpdate retorna o número de linhas afetadas
                int rows = ps.executeUpdate();
                if (rows > 0) System.out.println("Utilizador atualizado com sucesso!");
                else System.out.println("Utilizador não encontrado."); // Caso o ID não exista
            } catch (SQLException e) { e.printStackTrace(); }
            db.close();
        }
    }

    // --- ELIMINAÇÃO (DELETE) ---
    public static void eliminarUtilizador(Scanner scanner) {
        System.out.print("ID do Utilizador a eliminar: ");
        int id = scanner.nextInt();

        db = new DBConnection(dbName);
        if (db.connect()) {
            String sql = "DELETE FROM utilizadores WHERE id_utilizador = ?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) System.out.println("Utilizador eliminado com sucesso!");
                else System.out.println("Utilizador não encontrado.");
            } catch (SQLException e) {
                // Captura erro caso tente apagar alguém que tem treinos associados (Integridade Referencial)
                System.out.println("Erro: Não pode eliminar utilizadores com treinos registados.");
            }
            db.close();
        }
    }

    // --- PESQUISA (SEARCH) ---
    public static void pesquisarUtilizador(Scanner scanner) {
        System.out.print("Digite o nome (ou parte do nome) a pesquisar: ");
        String pesquisa = scanner.nextLine();

        db = new DBConnection(dbName);
        if (db.connect()) {
            // LIKE permite pesquisar por padrões
            String sql = "SELECT * FROM utilizadores WHERE nome LIKE ?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                // Adiciona % antes e depois para encontrar o texto em qualquer parte do nome
                ps.setString(1, "%" + pesquisa + "%");
                ResultSet rs = ps.executeQuery();
                System.out.println("\n--- RESULTADOS DA PESQUISA ---");
                while (rs.next()) {
                    System.out.printf("ID: %d | Nome: %s | Email: %s\n",
                            rs.getInt("id_utilizador"), rs.getString("nome"), rs.getString("email"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            db.close();
        }
    }

    // --- RELATÓRIO COM JOIN ---
    public static void relatorioTreinos() {
        db = new DBConnection(dbName);
        if (db.connect()) {
            // Query avançada: Junta Utilizadores -> Treinos -> Itens -> Exercicios
            String sql = "SELECT u.nome AS atleta, t.descricao, e.nome AS exercicio, i.series, i.repeticoes, i.carga_kg " +
                    "FROM itens_treino i " +
                    "JOIN treinos t ON i.id_treino = t.id_treino " +
                    "JOIN utilizadores u ON t.id_utilizador = u.id_utilizador " +
                    "JOIN exercicios e ON i.id_exercicio = e.id_exercicio " +
                    "ORDER BY t.data_treino DESC";

            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                System.out.println("\n--- RELATÓRIO DETALHADO DE TREINOS ---");
                // Formatação de tabela para ficar bonito no relatório
                System.out.printf("%-20s | %-20s | %-15s | %s | %s | %s\n",
                        "Atleta", "Treino", "Exercicio", "Sér.", "Reps", "Carga");
                System.out.println("--------------------------------------------------------------------------------------");

                while (rs.next()) {
                    System.out.printf("%-20s | %-20s | %-15s | %-4d | %-4d | %-5.1f kg\n",
                            rs.getString("atleta"),
                            rs.getString("descricao"),
                            rs.getString("exercicio"),
                            rs.getInt("series"),
                            rs.getInt("repeticoes"),
                            rs.getDouble("carga_kg"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            db.close();
        }
    }

    // --- INSERÇÃO (CREATE) ---
    public static void inserirUtilizador(Scanner scanner) {
        System.out.print("Nome do Utilizador: ");
        String nome = scanner.nextLine();
        System.out.print("Email do Utilizador: ");
        String email = scanner.nextLine();

        db = new DBConnection(dbName);
        if (db.connect()) {
            String sql = "INSERT INTO utilizadores (nome, email) VALUES (?, ?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.setString(2, email);
                ps.executeUpdate();
                System.out.println("Utilizador inserido com sucesso!");
            } catch (SQLException e) {
                System.out.println("Erro: Email já existe ou erro no SQL.");
            }
            db.close();
        }
    }

    // --- LISTAGEM SIMPLES ---
    public static void listarExercicios() {
        db = new DBConnection(dbName);
        if (db.connect()) {
            try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM exercicios")) {
                ResultSet rs = ps.executeQuery();
                System.out.println("\n--- LISTA DE EXERCÍCIOS ---");
                while (rs.next()) {
                    System.out.printf("Exercicio: %s | Grupo: %s\n",
                            rs.getString("nome"), rs.getString("grupo_muscular"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            db.close();
        }
    }
    public static void criarTreino(Scanner scanner) {
        // 1. Mostrar utilizadores para saber o ID
        listarUtilizadores();

        System.out.println("\n--- CRIAR NOVO TREINO ---");
        System.out.print("ID do Utilizador (Atleta): ");
        int idUtilizador = scanner.nextInt();
        scanner.nextLine(); // Limpar buffer

        System.out.print("Descrição do Treino (ex: Costas e Biceps): ");
        String descricao = scanner.nextLine();

        db = new DBConnection(dbName);
        if (db.connect()) {
            String sql = "INSERT INTO treinos (id_utilizador, descricao) VALUES (?, ?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUtilizador);
                ps.setString(2, descricao);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    // Recuperar o ID do treino gerado para mostrar ao utilizador
                    ResultSet generatedKeys = ps.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        System.out.println("Treino criado com sucesso! ID do Treino: " + generatedKeys.getInt(1));
                        System.out.println("Guarde este ID para adicionar exercícios.");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Erro ao criar treino: " + e.getMessage());
            }
            db.close();
        }
    }
    public static void adicionarItemTreino(Scanner scanner) {
        // 1. Pedir o ID do Treino
        System.out.println("\n--- ADICIONAR EXERCÍCIO AO TREINO ---");
        System.out.print("Digite o ID do Treino: ");
        int idTreino = scanner.nextInt();

        // 2. Mostrar exercícios disponíveis para facilitar
        listarExercicios();

        System.out.print("Digite o ID do Exercício a adicionar: ");
        int idExercicio = scanner.nextInt();

        System.out.print("Número de Séries (ex: 3): ");
        int series = scanner.nextInt();

        System.out.print("Número de Repetições (ex: 12): ");
        int repeticoes = scanner.nextInt();

        System.out.print("Carga em KG (ex: 20,5): ");
        double carga = scanner.nextDouble(); // Usa vírgula ou ponto dependendo do sistema

        db = new DBConnection(dbName);
        if (db.connect()) {
            String sql = "INSERT INTO itens_treino (id_treino, id_exercicio, series, repeticoes, carga_kg) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setInt(1, idTreino);
                ps.setInt(2, idExercicio);
                ps.setInt(3, series);
                ps.setInt(4, repeticoes);
                ps.setDouble(5, carga);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Exercício adicionado ao treino com sucesso!");
                }
            } catch (SQLException e) {
                System.out.println("Erro ao adicionar item: " + e.getMessage());
            }
            db.close();
        }
    }
}