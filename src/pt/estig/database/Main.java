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
            System.out.println("5. Editar Utilizador");          // Funcionalidade Adicionada
            System.out.println("6. Eliminar Utilizador");        // Funcionalidade Adicionada
            System.out.println("7. Pesquisar Utilizador por Nome"); // Funcionalidade Adicionada
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
            // JOIN combina a tabela treinos e utilizadores para mostrar o NOME em vez do ID numérico
            String sql = "SELECT u.nome, t.descricao, t.data_treino FROM treinos t " +
                    "JOIN utilizadores u ON t.id_utilizador = u.id_utilizador";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                System.out.println("\n--- HISTÓRICO DE TREINOS ---");
                while (rs.next()) {
                    System.out.printf("Atleta: %s | Treino: %s | Data: %s\n",
                            rs.getString("nome"), rs.getString("descricao"), rs.getTimestamp("data_treino"));
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
}